package com.sinop.minimuv.core

import android.content.Context
import com.sinop.minimuv.data.EpisodeNote
import com.sinop.minimuv.data.ProfileRepository
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleNote
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.data.WatchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

// ── Gizli not (partner ping) bildirimleri ─────────────────────────────────

object PingNotifier {

    private const val MAX_MEMORY_IDS = 200
    private val recentIds: MutableSet<String> =
        Collections.synchronizedSet(LinkedHashSet<String>())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            if (ping.fromProfile == profileId) return@forEach
            val sender = runCatching { profileRepo.getProfile(ping.fromProfile) }.getOrNull()
            NotificationHelper.show(
                context,
                "💌 ${sender?.name ?: "Partnerin"} sana yazdı",
                ping.message,
                id = id.hashCode(),
            )
            if (ts > newLast) newLast = ts
        }
        if (newLast != lastSeen) {
            val finalLast = newLast
            scope.launch { runCatching { settings.saveLastPingAt(finalLast) } }
        }
    }

    private fun normalizeTs(raw: String?): String? {
        val text = raw ?: return null
        if (text.isBlank()) return null
        return runCatching { Instant.parse(text).toString() }.getOrNull() ?: text
    }

    private fun nowIso(): String = Instant.now().toString()
}

// ── Başlık ekleme / durum geçişi bildirimleri ─────────────────────────────

object TitleTransitionTracker {

    private val mutex = Mutex()
    private var initialized = false
    private var lastStatuses: Map<String, String> = emptyMap()
    private val json = Json { ignoreUnknownKeys = true }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Mevcut başlık listesini son anlık görüntüyle karşılaştırır;
     *  gösterilecek bildirimleri (başlık, metin) listesi olarak döner.
     *  Arayan taraf NotificationHelper.show ile gösterir. */
    suspend fun diff(context: Context, profileId: String, current: List<Title>): List<Pair<String, String>> =
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
            val notes = mutableListOf<Pair<String, String>>()
            val byId = current.associateBy { it.id }
            byId.values.forEach { t ->
                val old = lastStatuses[t.id]
                if (old == null) {
                    // Yeni eklenen başlık — kendi eklediğimiz için kendimize bildirim gitmez.
                    if (t.createdByProfileId != profileId) notes += newTitleNote(t)
                } else if (old != t.status) {
                    notes += statusNote(old, t)
                }
            }
            persist(context, current)
            return notes
        }

    private fun persist(context: Context, current: List<Title>) {
        lastStatuses = current.associate { it.id to it.status }
        val payload = runCatching {
            json.encodeToString(MapSerializer(String.serializer(), String.serializer()), lastStatuses)
        }.getOrNull()
        if (payload != null) {
            scope.launch {
                runCatching { SettingsStore(context).saveTitlesSnapshot(payload) }
            }
        }
    }

    private fun newTitleNote(t: Title): Pair<String, String> = when (t.status) {
        WatchStatus.PLAN.db -> "✨ ${t.title}" to "Sırada listesine yeni bir şey eklendi!"
        WatchStatus.WATCHING.db -> "▶️ ${t.title}" to "Yeni bir serüven başladı!"
        WatchStatus.COMPLETED.db -> "🎉 ${t.title}" to "Birlikte bitirdiniz! Tebrikler!"
        WatchStatus.REWATCHING.db -> "🔁 ${t.title}" to "Yeniden izlemeye başladınız!"
        WatchStatus.PAUSED.db -> "⏸️ ${t.title}" to "Duraklatılanlar listesine eklendi."
        WatchStatus.DROPPED.db -> "🚪 ${t.title}" to "Bırakılanlara eklendi."
        else -> "✨ ${t.title}" to "Yeni bir başlık eklendi!"
    }

    private fun statusNote(old: String, t: Title): Pair<String, String> {
        if (t.status == WatchStatus.WATCHING.db) return "▶️ ${t.title}" to "İzlemeye başladınız!"
        if (t.status == WatchStatus.COMPLETED.db) return "🎉 ${t.title}" to "Birlikte bitirdiniz! Tebrikler!"
        if (t.status == WatchStatus.REWATCHING.db) return "🔁 ${t.title}" to "Yeniden izlemeye başladınız!"
        if (t.status == WatchStatus.PAUSED.db) return "⏸️ ${t.title}" to "Duraklatıldı — sonra devam ederiz."
        if (t.status == WatchStatus.DROPPED.db) return "🚪 ${t.title}" to "Bırakıldı olarak işaretlendi."
        if (t.status == WatchStatus.PLAN.db) return "🗓️ ${t.title}" to "Sıraya geri alındı."
        return "🔄 ${t.title}" to "Durum güncellendi."
    }
}

// ── Yıldönümü bildirimleri (günde bir kez) ────────────────────────────────

object AnniversaryChecker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Bugün için henüz kontrol edilmediyse yıldönümlerini bildirir. */
    suspend fun process(context: Context) {
        val settings = SettingsStore(context)
        val today = LocalDate.now().toString()
        if (settings.lastAnnivDate() == today) return
        val repo = TitleRepository()
        val titles = runCatching { repo.getTitles() }.getOrNull() ?: return
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
                            id = t.id.hashCode(),
                            channel = NotificationHelper.CHANNEL_ANNIVERSARY,
                        )
                    }
                }
            }
        }
        val done = today
        scope.launch { runCatching { settings.saveLastAnnivDate(done) } }
    }
}

// ── Bölüm kilometre taşları (ayrı ayrı izleme modu) ───────────────────────

object MilestoneTracker {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private var milestones: MutableMap<String, Int>? = null // null = henüz yüklenmedi

    /** Ayrı moddaki başlıklarda ikinizin de geçtiği 10'ar bölümlük kilometre
     *  taşlarını bildirir. Bildirilenler kalıcı kaydedilir — uygulama kapalıyken
     *  FCM/worker yolu çalışsa da aynı taş iki kez bildirilmez. */
    suspend fun process(context: Context, profileId: String) {
        val repo = TitleRepository()
        val titles = runCatching { repo.getTitles() }.getOrNull() ?: return
        mutex.withLock {
            if (milestones == null) {
                val stored = runCatching { SettingsStore(context).milestonesSnapshot() }.getOrNull()
                milestones = if (stored.isNullOrBlank()) {
                    mutableMapOf()
                } else {
                    runCatching { json.decodeFromString<Map<String, Int>>(stored) }
                        .getOrDefault(emptyMap())
                        .toMutableMap()
                }
            }
            var changed = false
            titles.forEach { title ->
                if (title.watchMode != "ayri") return@forEach
                val progress = runCatching { repo.getEpisodeProgress(title.id) }.getOrDefault(emptyList())
                val mine = progress.firstOrNull { it.profileId == profileId }?.currentEpisode ?: 0
                val partner = progress.firstOrNull { it.profileId != profileId }?.currentEpisode ?: 0
                val milestone = (minOf(mine, partner) / 10) * 10
                if (milestone >= 10 && milestones!![title.id] != milestone) {
                    milestones!![title.id] = milestone
                    changed = true
                    NotificationHelper.show(
                        context,
                        "🎊 ${title.title}",
                        "İkiniz de $milestone. bölümü geçtiniz!",
                        id = title.id.hashCode(),
                    )
                }
            }
            if (changed) {
                val payload = runCatching {
                    json.encodeToString(
                        MapSerializer(String.serializer(), Int.serializer()),
                        milestones!!,
                    )
                }.getOrNull()
                if (payload != null) {
                    val finalPayload = payload
                    scope.launch {
                        runCatching { SettingsStore(context).saveMilestonesSnapshot(finalPayload) }
                    }
                }
            }
        }
    }
}

// ── Partnerin puan değişiklikleri ─────────────────────────────────────────

object ScoreNotifier {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private var snapshot: MutableMap<String, String>? = null // "titleId|profileId" -> puan

    /** Partnerin yeni verdiği veya değiştirdiği puanları bildirir. */
    suspend fun process(context: Context, profileId: String) {
        val repo = TitleRepository()
        val scores = runCatching { repo.getAllTitleScores() }.getOrNull() ?: return
        val titles = runCatching { repo.getTitles() }.getOrNull().orEmpty().associateBy { it.id }
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
                val name = author?.name ?: "Partnerin"
                if (current.isNotBlank()) {
                    if (previous.isNullOrBlank()) {
                        NotificationHelper.show(
                            context,
                            "⭐ ${title?.title ?: "Bir yapım"}",
                            "$name ${String.format(java.util.Locale.US, "%.1f", current.toDouble())} puan verdi!",
                            id = key.hashCode(),
                        )
                    } else {
                        NotificationHelper.show(
                            context,
                            "⭐ ${title?.title ?: "Bir yapım"}",
                            "$name puanını ${String.format(java.util.Locale.US, "%.1f", current.toDouble())} olarak güncelledi.",
                            id = key.hashCode(),
                        )
                    }
                }
                snapshot!![key] = current
                changed = true
            }
            if (changed) persist(context)
        }
    }

    private fun persist(context: Context) {
        val payload = runCatching {
            json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()),
                snapshot!!,
            )
        }.getOrNull()
        if (payload != null) {
            val finalPayload = payload
            scope.launch { runCatching { SettingsStore(context).saveScoreSnapshot(finalPayload) } }
        }
    }
}

// ── Partnerin notları ──────────────────────────────────────────────────────

enum class NoteKind { TITLE, EPISODE }

object NoteNotifier {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
            scope.launch { runCatching { settings.saveLastTitleNoteAt(maxTs ?: nowIso()) } }
            return
        }
        val titles = runCatching { repo.getTitles() }.getOrNull().orEmpty().associateBy { it.id }
        val profiles = runCatching { ProfileRepository().getProfiles() }.getOrNull().orEmpty()
        var newLast: String = lastSeen
        notes.sortedBy { normalizeTs(it.createdAt) ?: "" }.forEach { note ->
            val ts = normalizeTs(note.createdAt) ?: return@forEach
            if (ts <= lastSeen || note.profileId == profileId) return@forEach
            if (ts > newLast) newLast = ts
            val author = profiles.firstOrNull { it.id == note.profileId }
            val title = titles[note.titleId]
            NotificationHelper.show(
                context,
                "💬 ${title?.title ?: "Bir yapım"}",
                "${author?.name ?: "Partnerin"} “${title?.title ?: "bir yapım"}” başlığına not ekledi.",
                id = note.id?.hashCode() ?: ts.hashCode(),
            )
        }
        if (newLast != lastSeen) {
            val finalLast = newLast
            scope.launch { runCatching { settings.saveLastTitleNoteAt(finalLast) } }
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
            scope.launch { runCatching { settings.saveLastEpisodeNoteAt(maxTs ?: nowIso()) } }
            return
        }
        val titles = runCatching { repo.getTitles() }.getOrNull().orEmpty().associateBy { it.id }
        val profiles = runCatching { ProfileRepository().getProfiles() }.getOrNull().orEmpty()
        val myProgressCache = mutableMapOf<String, Int>()
        var newLast: String = lastSeen
        notes.sortedBy { normalizeTs(it.createdAt) ?: "" }.forEach { note ->
            val ts = normalizeTs(note.createdAt) ?: return@forEach
            if (ts <= lastSeen || note.profileId == profileId) return@forEach
            if (ts > newLast) newLast = ts
            // Spoiler kilidi: o bölüme gelmediysem bildirim düşmez
            val myProgress = myProgressCache.getOrPut(note.titleId) {
                runCatching { repo.getEpisodeProgress(note.titleId) }
                    .getOrDefault(emptyList())
                    .firstOrNull { it.profileId == profileId }
                    ?.currentEpisode ?: 0
            }
            if (myProgress < note.episodeNumber) return@forEach
            val author = profiles.firstOrNull { it.id == note.profileId }
            val title = titles[note.titleId]
            NotificationHelper.show(
                context,
                "💬 ${title?.title ?: "Bir yapım"} • Bölüm ${note.episodeNumber}",
                "${author?.name ?: "Partnerin"} “${title?.title ?: "bir yapımın"}” ${note.episodeNumber}. bölümüne not ekledi.",
                id = note.id?.hashCode() ?: ts.hashCode(),
            )
        }
        if (newLast != lastSeen) {
            val finalLast = newLast
            scope.launch { runCatching { settings.saveLastEpisodeNoteAt(finalLast) } }
        }
    }

    private fun normalizeTs(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching { java.time.Instant.parse(raw).toString() }.getOrNull() ?: raw
    }

    private fun nowIso(): String = java.time.Instant.now().toString()
}
