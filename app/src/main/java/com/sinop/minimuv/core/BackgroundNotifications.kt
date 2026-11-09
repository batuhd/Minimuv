package com.sinop.minimuv.core

import android.content.Context
import com.sinop.minimuv.data.EpisodeNote
import com.sinop.minimuv.data.NotificationLogStore
import com.sinop.minimuv.data.NotificationMeta
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleNote
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.data.WatchMode
import com.sinop.minimuv.data.WatchStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.util.Collections
import java.util.LinkedHashSet

/**
 * Uygulama kapalıyken de çalışan (WorkManager) ve uygulama açıkken çalışan (realtime)
 * iki dinleyicinin ORTAK bildirim mantığı. Aynı süreç içinde kilitli tutarlı durum +
 * DataStore'da kalıcı iz sayesinde aynı olay iki kez bildirilmez.
 */

// ── Zaman damgası yardımcıları (dosya genelinde tek kopya) ────────────────

private fun normalizeTs(raw: String?): String? {
    val text = raw ?: return null
    if (text.isBlank()) return null
    return runCatching { Instant.parse(text).toString() }.getOrNull() ?: text
}

private fun nowIso(): String = Instant.now().toString()

// ── Gizli not (partner ping) bildirimleri ─────────────────────────────────

object PingNotifier {

    private const val MAX_MEMORY_IDS = 200
    private val recentIds: MutableSet<String> =
        Collections.synchronizedSet(LinkedHashSet<String>())

    /** Son 20 ping'e bakar, yeni olanları (son görülenden sonra gelenleri) bildirir.
     *  Kalıcı son-görülen zaman damgası sayesinde uygulama yeniden açılsa da
     *  worker ile aynı şeyler tekrar bildirilmez. */
    suspend fun process(context: Context, profileId: String) {
        val repo = TitleRepository()
        val profileRepo = ProfileRepository()
        val settings = SettingsStore(context)
        val pings = runCatching { repo.getRecentPings(20) }.getOrNull() ?: return

        val lastSeen: String = settings.lastPingAt() ?: run {
            // İlk çalıştırma: eski pingler bildirilmesin, sadece iz bırak.
            val maxTs = pings.mapNotNull { normalizeTs(it.createdAt) }.maxOrNull()
            settings.saveLastPingAt(maxTs ?: nowIso())
            return
        }

        var newLast: String = lastSeen
        pings.forEach { ping ->
            val id = ping.id ?: return@forEach
            val ts = normalizeTs(ping.createdAt) ?: return@forEach
            if (id in recentIds || ts <= lastSeen) return@forEach
            recentIds.add(id)
            if (recentIds.size > MAX_MEMORY_IDS) {
                recentIds.take(MAX_MEMORY_IDS / 2).forEach(recentIds::remove)
            }
            // Önce "son görülen"i kalıcı yaz, sonra göster: çökme/yeniden
            // başlatma durumunda aynı ping ikinci kez bildirilmez (BUG-006).
            if (ts > newLast) {
                newLast = ts
                runCatching { settings.saveLastPingAt(newLast) }
            }
            if (ping.fromProfile == profileId) return@forEach
            val sender = runCatching { profileRepo.getProfile(ping.fromProfile) }.getOrNull()
            val senderName = sender?.displayName ?: "Partnerin"
            NotificationHelper.show(
                context,
                "💌 $senderName sana yazdı",
                ping.message,
                id = id.hashCode(),
                meta = NotificationMeta(
                    type = "ping",
                    eventKey = "ping:$id",
                    entryTitleEn = "💌 $senderName wrote to you",
                    entryMessageEn = ping.message,
                ),
            )
        }
    }

}

// ── Başlık ekleme / durum geçişi bildirimleri ─────────────────────────────

/** Gösterilecek başlık bildirimi + gelen kutusu metadatası. */
data class TitleNotification(
    val title: String,
    val text: String,
    val titleEn: String,
    val textEn: String,
    val titleId: String,
    val eventKey: String,
) {
    val meta: NotificationMeta
        get() = NotificationMeta(
            type = eventKey.substringBefore(':'),
            eventKey = eventKey,
            titleId = titleId,
            entryTitleEn = titleEn,
            entryMessageEn = textEn,
        )
}

object TitleTransitionTracker {

    private val mutex = Mutex()
    private var initialized = false
    private var lastStatuses: Map<String, String> = emptyMap()
    private val json = Json { ignoreUnknownKeys = true }

    /** Mevcut başlık listesini son anlık görüntüyle karşılaştırır;
     *  gösterilecek bildirimleri (başlık, metin) listesi olarak döner.
     *  Arayan taraf NotificationHelper.show ile gösterir. */
    suspend fun diff(context: Context, profileId: String, current: List<Title>): List<TitleNotification> =
        mutex.withLock {
            if (!initialized) {
                initialized = true
                val stored = runCatching { SettingsStore(context).titlesSnapshot() }.getOrNull()
                if (stored == null) {
                    // İlk kez: eski kayıtlar için bildirim fırtınası çıkmasın, sessizce başlat.
                    persist(context, current)
                    return emptyList()
                }
                lastStatuses = runCatching { json.decodeFromString<Map<String, String>>(stored) }
                    .getOrDefault(emptyMap())
            }
            val notes = mutableListOf<TitleNotification>()
            val byId = current.associateBy { it.id }
            byId.values.forEach { t ->
                val old = lastStatuses[t.id]
                if (old == null) {
                    // Yeni eklenen başlık — kendi eklediğimiz için kendimize bildirim gitmez.
                    if (t.createdByProfileId != profileId) notes += newTitleNote(t)
                } else if (old != t.status) {
                    // Durumu kendi cihazımızda değiştirdiysek kendimize bildirim gösterme;
                    // partnerin değişikliği ise bildir (updated_by_profile_id).
                    if (t.updatedByProfileId != profileId) notes += statusNote(old, t)
                }
            }
            persist(context, current)
            return notes
        }

    private suspend fun persist(context: Context, current: List<Title>) {
        lastStatuses = current.associate { it.id to it.status }
        val payload = runCatching {
            json.encodeToString(MapSerializer(String.serializer(), String.serializer()), lastStatuses)
        }.getOrNull()
        if (payload != null) {
            // Bekleyerek yaz: worker dönmeden kalıcı olsun (BUG-006).
            runCatching { SettingsStore(context).saveTitlesSnapshot(payload) }
        }
    }

    private fun newTitleNote(t: Title): TitleNotification {
        val key = "title_new:${t.id}:${t.status}"
        return when (t.status) {
            WatchStatus.PLAN.db -> TitleNotification(
                "✨ ${t.title}", "Sırada listesine yeni bir şey eklendi!",
                "✨ ${t.title}", "Something new was added to the watchlist!", t.id, key,
            )
            WatchStatus.WATCHING.db -> TitleNotification(
                "▶️ ${t.title}", "Yeni bir serüven başladı!",
                "▶️ ${t.title}", "A new adventure begins!", t.id, key,
            )
            WatchStatus.COMPLETED.db -> TitleNotification(
                "🎉 ${t.title}", "Birlikte bitirdiniz! Tebrikler!",
                "🎉 ${t.title}", "You finished it together! Congrats!", t.id, key,
            )
            WatchStatus.REWATCHING.db -> TitleNotification(
                "🔁 ${t.title}", "Yeniden izlemeye başladınız!",
                "🔁 ${t.title}", "You're rewatching it!", t.id, key,
            )
            WatchStatus.PAUSED.db -> TitleNotification(
                "⏸️ ${t.title}", "Duraklatılanlar listesine eklendi.",
                "⏸️ ${t.title}", "Added to the paused list.", t.id, key,
            )
            WatchStatus.DROPPED.db -> TitleNotification(
                "🚪 ${t.title}", "Bırakılanlara eklendi.",
                "🚪 ${t.title}", "Moved to dropped.", t.id, key,
            )
            else -> TitleNotification(
                "✨ ${t.title}", "Yeni bir başlık eklendi!",
                "✨ ${t.title}", "A new title was added!", t.id, key,
            )
        }
    }

    private fun statusNote(old: String, t: Title): TitleNotification {
        val key = "title_status:${t.id}:$old->${t.status}"
        if (t.status == WatchStatus.WATCHING.db) return TitleNotification(
            "▶️ ${t.title}", "İzlemeye başladınız!",
            "▶️ ${t.title}", "You started watching!", t.id, key,
        )
        if (t.status == WatchStatus.COMPLETED.db) return TitleNotification(
            "🎉 ${t.title}", "Birlikte bitirdiniz! Tebrikler!",
            "🎉 ${t.title}", "You finished it together! Congrats!", t.id, key,
        )
        if (t.status == WatchStatus.REWATCHING.db) return TitleNotification(
            "🔁 ${t.title}", "Yeniden izlemeye başladınız!",
            "🔁 ${t.title}", "You're rewatching it!", t.id, key,
        )
        if (t.status == WatchStatus.PAUSED.db) return TitleNotification(
            "⏸️ ${t.title}", "Duraklatıldı — sonra devam ederiz.",
            "⏸️ ${t.title}", "Paused — we'll continue later.", t.id, key,
        )
        if (t.status == WatchStatus.DROPPED.db) return TitleNotification(
            "🚪 ${t.title}", "Bırakıldı olarak işaretlendi.",
            "🚪 ${t.title}", "Marked as dropped.", t.id, key,
        )
        if (t.status == WatchStatus.PLAN.db) return TitleNotification(
            "🗓️ ${t.title}", "Sıraya geri alındı.",
            "🗓️ ${t.title}", "Moved back to the watchlist.", t.id, key,
        )
        return TitleNotification(
            "🔄 ${t.title}", "Durum güncellendi.",
            "🔄 ${t.title}", "Status updated.", t.id, key,
        )
    }
}

// ── Yıldönümü bildirimleri (günde bir kez) ────────────────────────────────

object AnniversaryChecker {

    /** Bugün için henüz kontrol edilmediyse yıldönümlerini bildirir. */
    suspend fun process(context: Context) {
        val settings = SettingsStore(context)
        val today = LocalDate.now().toString()
        if (settings.lastAnnivDate() == today) return
        val repo = TitleRepository()
        val titles = runCatching { repo.getTitlesLite() }
            .onFailure { android.util.Log.w("MinimuvNotif", "Başlıklar yüklenemedi", it) }
            .getOrNull() ?: return
        val todayDate = LocalDate.now()
        titles.forEach { t ->
            val date = t.startDate ?: return@forEach
            runCatching {
                val d = LocalDate.parse(date)
                if (d.monthValue == todayDate.monthValue &&
                    d.dayOfMonth == todayDate.dayOfMonth &&
                    d.year < todayDate.year
                ) {
                    val years = Period.between(d, todayDate).years
                    if (years >= 1) {
                        NotificationHelper.show(
                            context,
                            "💌 Tam $years yıl önce bugün",
                            "${t.title} dizisine başlamıştık… Ne günlerdi!",
                            id = ("anniv_" + t.id).hashCode(),
                            channel = NotificationHelper.CHANNEL_ANNIVERSARY,
                            meta = NotificationMeta(
                                type = "anniversary",
                                eventKey = "anniv:${t.id}:$today",
                                titleId = t.id,
                                entryTitleEn = "💌 Exactly $years years ago today",
                                entryMessageEn = "We started ${t.title}… Those were the days!",
                            ),
                        )
                    }
                }
            }
        }
        val done = today
        runCatching { settings.saveLastAnnivDate(done) }
    }
}

// ── Bölüm kilometre taşları (ayrı ayrı izleme modu) ───────────────────────

object MilestoneTracker {

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private var milestones: MutableMap<String, Int>? = null // titleId -> bildirilen taş
    // "titleId|profileId" -> son görülen bölüm. Kendi eklediğimiz bölümün
    // bildirim üretmemesi için hangi tarafın ilerlediğini ayırt eder.
    private var progressSnapshot: MutableMap<String, Int>? = null
    private var canNotify = false

    /** Ayrı moddaki başlıklarda ikinizin de geçtiği 10'ar bölümlük kilometre
     *  taşlarını bildirir. Bildirilenler kalıcı kaydedilir — uygulama kapalıyken
     *  FCM/worker yolu çalışsa da aynı taş iki kez bildirilmez.
     *
     *  Bildirim yalnızca taşı **partner** geçirdiğinde gösterilir. Kendi
     *  eklediğimiz bölüm taşı geçirse bile taş sessizce kaydedilir; böylece
     *  kendi cihazımızda kendi işlemimiz için bildirim çıkmaz. */
    suspend fun process(context: Context, profileId: String) {
        val repo = TitleRepository()
        val titles = runCatching { repo.getTitlesLite() }
            .onFailure { android.util.Log.w("MinimuvNotif", "Başlıklar yüklenemedi", it) }
            .getOrNull() ?: return
        mutex.withLock {
            val settings = SettingsStore(context)
            if (milestones == null) {
                val storedMilestones = runCatching { settings.milestonesSnapshot() }.getOrNull()
                val storedProgress = runCatching { settings.milestoneProgressSnapshot() }.getOrNull()
                milestones = decodeIntMap(storedMilestones)
                progressSnapshot = decodeIntMap(storedProgress)
                // İlk kurulumda / eski sürümden geçişte mevcut durum sessizce
                // fotoğraflanır: geçmiş kilometre taşları için bildirim fırtınası yok.
                canNotify = !storedMilestones.isNullOrBlank() && !storedProgress.isNullOrBlank()
            }
            val ayriTitles = titles.filter { it.watchMode == WatchMode.AYRI.db }
            if (ayriTitles.isEmpty()) return@withLock
            // Tek sorgu: başlık başına ayrı istek (N+1) yerine tüm ilerlemeler bir kez çekilir.
            val progressByTitle = runCatching { repo.getAllEpisodeProgress() }
                .getOrDefault(emptyList())
                .groupBy { it.titleId }
            var changedMilestones = false
            var changedProgress = false
            ayriTitles.forEach { title ->
                val rows = progressByTitle[title.id].orEmpty()
                val myRow = rows.firstOrNull { it.profileId == profileId }
                val partnerRow = rows.firstOrNull { it.profileId != profileId }
                val mine = myRow?.currentEpisode ?: 0
                val partner = partnerRow?.currentEpisode ?: 0
                val milestone = (minOf(mine, partner) / 10) * 10

                val myKey = "${title.id}|$profileId"
                val partnerKey = partnerRow?.let { "${title.id}|${it.profileId}" }
                val previousPartner = partnerKey?.let { progressSnapshot!![it] }

                if (milestone >= 10 && milestones!![title.id] != milestone) {
                    milestones!![title.id] = milestone
                    changedMilestones = true
                    // Taşı partner mi geçirdi? Önceki fotoğraf yoksa (ilk kez
                    // görülen satır) 0 kabul edilir; ilk kurulum zaten sessiz.
                    val partnerMoved = partnerRow != null && partner > (previousPartner ?: 0)
                    if (canNotify && partnerMoved) {
                        NotificationHelper.show(
                            context,
                            "🎊 ${title.title}",
                            "İkiniz de $milestone. bölümü geçtiniz!",
                            id = title.id.hashCode(),
                            meta = NotificationMeta(
                                type = "milestone",
                                eventKey = "milestone:${title.id}:$milestone",
                                titleId = title.id,
                                entryMessageEn = "You both passed episode $milestone!",
                            ),
                        )
                    }
                }

                fun remember(key: String, value: Int) {
                    if (progressSnapshot!![key] != value) {
                        progressSnapshot!![key] = value
                        changedProgress = true
                    }
                }
                remember(myKey, mine)
                if (partnerKey != null) remember(partnerKey, partner)
            }
            if (changedMilestones) {
                persist(settings::saveMilestonesSnapshot, milestones!!)
            }
            if (changedProgress) {
                persist(settings::saveMilestoneProgressSnapshot, progressSnapshot!!)
            }
        }
    }

    private fun decodeIntMap(raw: String?): MutableMap<String, Int> =
        if (raw.isNullOrBlank()) {
            mutableMapOf()
        } else {
            runCatching { json.decodeFromString<Map<String, Int>>(raw) }
                .getOrDefault(emptyMap())
                .toMutableMap()
        }

    private suspend fun persist(
        save: suspend (String) -> Unit,
        value: Map<String, Int>,
    ) {
        val payload = runCatching {
            json.encodeToString(MapSerializer(String.serializer(), Int.serializer()), value)
        }.getOrNull() ?: return
        // Bekleyerek yaz: worker dönmeden kalıcı olsun (BUG-006).
        runCatching { save(payload) }
    }
}

// ── Partnerin puan değişiklikleri ─────────────────────────────────────────

object ScoreNotifier {

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private var snapshot: MutableMap<String, String>? = null // "titleId|profileId" -> puan

    /** Partnerin yeni verdiği veya değiştirdiği puanları bildirir. */
    suspend fun process(context: Context, profileId: String) {
        val repo = TitleRepository()
        val scores = runCatching { repo.getAllTitleScores() }.getOrNull() ?: return
        val titles = runCatching { repo.getTitlesLite() }
            .onFailure { android.util.Log.w("MinimuvNotif", "Başlıklar yüklenemedi", it) }
            .getOrNull().orEmpty().associateBy { it.id }
        val profiles = runCatching { ProfileRepository().getProfiles() }.getOrNull().orEmpty()

        mutex.withLock {
            if (snapshot == null) {
                val stored = runCatching { SettingsStore(context).scoreSnapshot() }.getOrNull()
                if (stored.isNullOrBlank()) {
                    // İlk çalıştırma: eski puanlar için bildirim fırtınası çıkmasın
                    snapshot = scores.associate {
                        "${it.titleId}|${it.profileId}" to (it.score?.toString() ?: "")
                    }.toMutableMap()
                    persist(context)
                    return
                }
                snapshot = runCatching { json.decodeFromString<Map<String, String>>(stored) }
                    .getOrDefault(emptyMap())
                    .toMutableMap()
            }

            var changed = false
            scores.forEach { s ->
                val key = "${s.titleId}|${s.profileId}"
                val current = s.score?.toString() ?: ""
                val previous = snapshot!![key]

                // Kendi puanım için sadece izi güncelle — kendime bildirim yok
                if (s.profileId == profileId) {
                    if (previous != current) {
                        snapshot!![key] = current
                        changed = true
                    }
                    return@forEach
                }
                if (previous == null && current.isBlank()) return@forEach
                if (previous != null && previous == current) return@forEach

                val title = titles[s.titleId]
                val author = profiles.firstOrNull { it.id == s.profileId }
                val name = author?.displayName ?: "Partnerin"
                val scoreText = String.format(java.util.Locale.US, "%.1f", current.toDouble())
                if (current.isNotBlank()) {
                    val meta = NotificationMeta(
                        type = "score",
                        eventKey = "score:$key:$current",
                        titleId = s.titleId,
                        entryMessageEn = if (previous.isNullOrBlank()) {
                            "$name rated it $scoreText!"
                        } else {
                            "$name updated their rating to $scoreText."
                        },
                    )
                    if (previous.isNullOrBlank()) {
                        NotificationHelper.show(
                            context,
                            "⭐ ${title?.title ?: "Bir yapım"}",
                            "$name $scoreText puan verdi!",
                            id = key.hashCode(),
                            meta = meta,
                        )
                    } else {
                        NotificationHelper.show(
                            context,
                            "⭐ ${title?.title ?: "Bir yapım"}",
                            "$name puanını $scoreText olarak güncelledi.",
                            id = key.hashCode(),
                            meta = meta,
                        )
                    }
                }
                snapshot!![key] = current
                changed = true
            }
            if (changed) persist(context)
        }
    }

    private suspend fun persist(context: Context) {
        val payload = runCatching {
            json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                snapshot!!,
            )
        }.getOrNull()
        if (payload != null) {
            // Bekleyerek yaz: worker dönmeden kalıcı olsun (BUG-006).
            runCatching { SettingsStore(context).saveScoreSnapshot(payload) }
        }
    }
}

// ── Partnerin notları ──────────────────────────────────────────────────────

enum class NoteKind { TITLE, EPISODE }

object NoteNotifier {

    private val mutex = Mutex()

    /** Partnerin yeni notlarını bildirir. Bölüm notlarında spoiler kuralı
     *  geçerlidir: o bölüme gelmediysen bildirim hiç düşmez. */
    suspend fun process(context: Context, profileId: String, kind: NoteKind) {
        val repo = TitleRepository()
        val settings = SettingsStore(context)

        mutex.withLock {
            when (kind) {
                NoteKind.TITLE -> processTitleNotes(context, profileId, repo, settings)
                NoteKind.EPISODE -> processEpisodeNotes(context, profileId, repo, settings)
            }
        }
    }

    private suspend fun processTitleNotes(
        context: Context,
        profileId: String,
        repo: TitleRepository,
        settings: SettingsStore,
    ) {
        val notes = runCatching { repo.getAllTitleNotes() }.getOrNull() ?: return
        val lastSeen: String = settings.lastTitleNoteAt() ?: run {
            // İlk çalıştırma: eski notlar bildirilmesin, sadece iz bırak.
            val maxTs = notes.mapNotNull { normalizeTs(it.createdAt) }.maxOrNull()
            settings.saveLastTitleNoteAt(maxTs ?: nowIso())
            return
        }
        val titles = runCatching { repo.getTitlesLite() }
            .onFailure { android.util.Log.w("MinimuvNotif", "Başlıklar yüklenemedi", it) }
            .getOrNull().orEmpty().associateBy { it.id }
        val profiles = runCatching { ProfileRepository().getProfiles() }.getOrNull().orEmpty()
        var newLast: String = lastSeen
        notes.sortedBy { normalizeTs(it.createdAt) ?: "" }.forEach { note ->
            val ts = normalizeTs(note.createdAt) ?: return@forEach
            if (ts <= lastSeen || note.profileId == profileId) return@forEach
            // Önce "son görülen"i kalıcı yaz, sonra göster (BUG-006).
            if (ts > newLast) {
                newLast = ts
                runCatching { settings.saveLastTitleNoteAt(newLast) }
            }
            val author = profiles.firstOrNull { it.id == note.profileId }
            val title = titles[note.titleId]
            val titleName = title?.title ?: "bir yapım"
            val authorName = author?.displayName ?: "Partnerin"
            val content = note.noteText.takeIf { it.isNotBlank() }
            NotificationHelper.show(
                context,
                "💬 $titleName",
                "$authorName “$titleName” başlığına not ekledi.",
                id = note.id?.hashCode() ?: ts.hashCode(),
                meta = NotificationMeta(
                    type = "note",
                    eventKey = "note:${note.id ?: ts}",
                    titleId = note.titleId,
                    entryMessage = content,
                    entryMessageEn = content ?: "$authorName added a note to “$titleName”.",
                ),
            )
        }
    }

    private suspend fun processEpisodeNotes(
        context: Context,
        profileId: String,
        repo: TitleRepository,
        settings: SettingsStore,
    ) {
        val notes = runCatching { repo.getAllEpisodeNotes() }.getOrNull() ?: return
        val lastSeen: String = settings.lastEpisodeNoteAt() ?: run {
            // İlk çalıştırma: eski notlar bildirilmesin, sadece iz bırak.
            val maxTs = notes.mapNotNull { normalizeTs(it.createdAt) }.maxOrNull()
            settings.saveLastEpisodeNoteAt(maxTs ?: nowIso())
            return
        }
        val titles = runCatching { repo.getTitlesLite() }
            .onFailure { android.util.Log.w("MinimuvNotif", "Başlıklar yüklenemedi", it) }
            .getOrNull().orEmpty().associateBy { it.id }
        val profiles = runCatching { ProfileRepository().getProfiles() }.getOrNull().orEmpty()
        val myProgressCache = mutableMapOf<String, Int>()
        var newLast: String = lastSeen
        notes.sortedBy { normalizeTs(it.createdAt) ?: "" }.forEach { note ->
            val ts = normalizeTs(note.createdAt) ?: return@forEach
            if (ts <= lastSeen || note.profileId == profileId) return@forEach
            // Spoiler kilidi: o bölüme gelmediysem bildirim düşmez
            val myProgress = myProgressCache.getOrPut(note.titleId) {
                runCatching { repo.getEpisodeProgress(note.titleId) }
                    .getOrDefault(emptyList())
                    .firstOrNull { it.profileId == profileId }
                    ?.currentEpisode ?: 0
            }
            val author = profiles.firstOrNull { it.id == note.profileId }
            val title = titles[note.titleId]
            val titleName = title?.title ?: "bir yapım"
            val authorName = author?.displayName ?: "Partnerin"
            val content = note.noteText?.takeIf { it.isNotBlank() } ?: note.emojiReaction?.takeIf { it.isNotBlank() }
            val eventKey = "episode_note:${note.id ?: ts}"
            if (myProgress < note.episodeNumber) {
                // Spoiler kilidi: bildirim gösterilmez (mevcut davranış), ama
                // gelen kutusuna içerik sızdırmadan kilitli kayıt düşer. Su
                // işareti ilerlemez; bölüme gelince gerçek metinle güncellenir
                // (BUG-033).
                runCatching {
                    NotificationLogStore(context).log(
                        meta = NotificationMeta(
                            type = "episode_note",
                            eventKey = eventKey,
                            titleId = note.titleId,
                            entryTitle = "💬 $titleName • Bölüm ${note.episodeNumber}",
                            entryTitleEn = "💬 $titleName • Episode ${note.episodeNumber}",
                            entryMessage = content,
                            entryMessageEn = content,
                            lockedMessage =
                                "🔒 $authorName ${note.episodeNumber}. bölüm için bir not bıraktı. O bölüme gelince görünecek.",
                            lockedMessageEn =
                                "🔒 $authorName left a note for episode ${note.episodeNumber}. Watch it to reveal.",
                            locked = true,
                            episodeNumber = note.episodeNumber,
                        ),
                        shownTitle = "",
                        shownMessage = "",
                    )
                }
                return@forEach
            }
            // Önce "son görülen"i kalıcı yaz, sonra göster (BUG-006).
            // Su işareti yalnızca gerçekten gösterilen notta ilerler; spoiler
            // nedeniyle atlanan not ileride tekrar değerlendirilir (BUG-033).
            if (ts > newLast) {
                newLast = ts
                runCatching { settings.saveLastEpisodeNoteAt(newLast) }
            }
            NotificationHelper.show(
                context,
                "💬 $titleName • Bölüm ${note.episodeNumber}",
                "$authorName “$titleName” ${note.episodeNumber}. bölümüne not ekledi.",
                id = note.id?.hashCode() ?: ts.hashCode(),
                meta = NotificationMeta(
                    type = "episode_note",
                    eventKey = eventKey,
                    titleId = note.titleId,
                    entryTitle = "💬 $titleName • Bölüm ${note.episodeNumber}",
                    entryTitleEn = "💬 $titleName • Episode ${note.episodeNumber}",
                    entryMessage = content,
                    entryMessageEn = content,
                    episodeNumber = note.episodeNumber,
                ),
            )
        }
    }

}
