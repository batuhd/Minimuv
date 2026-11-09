package com.sinop.minimuv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Cihazda gösterilen bildirimlerin yerel gelen kutusu.
 *
 * Kayıt noktası tek: `NotificationHelper.show` — realtime, FCM ve WorkManager
 * yollarının hepsi oradan geçer. Burada yalnızca gerçekten gösterilen bildirim
 * yazılır; gösterilmeyen (izin yok / çift bildirim koruması) bir olay kayda girmez.
 */
@Serializable
data class NotificationEntry(
    val id: String,
    val type: String,
    /** Ekranda gösterilecek başlık (TR). */
    val title: String,
    /** Kilit açıksa gösterilecek gövde (TR). */
    val message: String,
    val titleEn: String? = null,
    val messageEn: String? = null,
    /** Bölüm notu kilidindeyken gösterilen metin — içerik sızdırmaz. */
    val lockedMessage: String? = null,
    val lockedMessageEn: String? = null,
    val titleId: String? = null,
    /** Aynı olayın iki kez kaydedilmesini engelleyen anahtar. */
    val eventKey: String,
    val createdAt: Long,
    val read: Boolean = false,
    val locked: Boolean = false,
    val episodeNumber: Int? = null,
)

/**
 * [NotificationHelper.show] çağrılarına eklenen kayıt bilgisi. Gösterilecek
 * metin varsayılan olarak bildirimin kendi metnidir; gelen kutusunda farklı bir
 * gövde gösterilmesi gereken durumlar (ör. bölüm notunun asıl içeriği)
 * entryMessage/entryMessageEn ile geçilir.
 */
data class NotificationMeta(
    val type: String,
    val eventKey: String,
    val titleId: String? = null,
    val entryTitle: String? = null,
    val entryMessage: String? = null,
    val entryTitleEn: String? = null,
    val entryMessageEn: String? = null,
    val lockedMessage: String? = null,
    val lockedMessageEn: String? = null,
    val locked: Boolean = false,
    val episodeNumber: Int? = null,
)

/** Liste ekranında tutulacak en fazla kayıt; en eskiler düşer. */
private const val MAX_ENTRIES = 200

private val Context.notificationDataStore by preferencesDataStore(name = "minimuv_notifications")

class NotificationLogStore(private val context: Context) {

    private val key = stringPreferencesKey("entries")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Oku-değiştir-yaz yarışlarını serileştirir: aynı anda iki bildirim gelse
    // bile biri diğerinin yazdığını ezmez.
    private val mutex = Mutex()
    private val listSerializer = ListSerializer(NotificationEntry.serializer())

    val entries: Flow<List<NotificationEntry>> = context.notificationDataStore.data.map { prefs ->
        decode(prefs[key])
    }

    val unreadCount: Flow<Int> = entries.map { list -> list.count { !it.read } }

    private fun decode(raw: String?): List<NotificationEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())
    }

    private suspend fun read(): List<NotificationEntry> =
        decode(context.notificationDataStore.data.first()[key])

    private suspend fun write(list: List<NotificationEntry>) {
        val sorted = list.sortedByDescending { it.createdAt }
        val capped = if (sorted.size > MAX_ENTRIES) sorted.take(MAX_ENTRIES) else sorted
        val payload = json.encodeToString(listSerializer, capped)
        context.notificationDataStore.edit { it[key] = payload }
    }

    /**
     * Yeni olayı kaydeder. Aynı [NotificationMeta.eventKey] daha önce yazıldıysa
     * tekrar eklenmez; yalnızca kilitli bir bölüm notu artık gösterilebilir hale
     * geldiyse mevcut kayıt güncellenir (kimlik/tarih/okundu durumu korunur).
     */
    suspend fun log(meta: NotificationMeta, shownTitle: String, shownMessage: String) = mutex.withLock {
        val list = read().toMutableList()
        val entryTitle = meta.entryTitle ?: shownTitle
        val entryMessage = meta.entryMessage ?: shownMessage
        val index = list.indexOfFirst { it.eventKey == meta.eventKey }
        if (index >= 0) {
            val old = list[index]
            if (old.locked && !meta.locked) {
                list[index] = old.copy(
                    title = entryTitle,
                    message = entryMessage,
                    titleEn = meta.entryTitleEn ?: entryTitle,
                    messageEn = meta.entryMessageEn ?: entryMessage,
                    locked = false,
                )
                write(list)
            }
            return@withLock
        }
        list.add(
            NotificationEntry(
                id = UUID.randomUUID().toString(),
                type = meta.type,
                title = entryTitle,
                message = entryMessage,
                titleEn = meta.entryTitleEn ?: entryTitle,
                messageEn = meta.entryMessageEn ?: entryMessage,
                lockedMessage = meta.lockedMessage,
                lockedMessageEn = meta.lockedMessageEn,
                titleId = meta.titleId,
                eventKey = meta.eventKey,
                createdAt = System.currentTimeMillis(),
                read = false,
                locked = meta.locked,
                episodeNumber = meta.episodeNumber,
            ),
        )
        write(list)
    }

    suspend fun markRead(id: String) = mutex.withLock {
        val list = read()
        if (list.none { it.id == id && !it.read }) return@withLock
        write(list.map { if (it.id == id) it.copy(read = true) else it })
    }

    suspend fun markAllRead() = mutex.withLock {
        val list = read()
        if (list.none { !it.read }) return@withLock
        write(list.map { if (it.read) it else it.copy(read = true) })
    }

    suspend fun clear() = mutex.withLock {
        context.notificationDataStore.edit { it.remove(key) }
    }

    /**
     * Partnerin izleme ilerlemesi kilitli bölüm notuna ulaştıysa kilidi açar.
     * Bildirim gösterilmemiş olsa bile (izin kapalı) içerik görünür olur.
     */
    suspend fun refreshEpisodeUnlocks(profileId: String) {
        val lockedTitleIds = read().filter { it.locked }.mapNotNull { it.titleId }.distinct()
        if (lockedTitleIds.isEmpty()) return
        // Ağ çağrıları kilit dışında: bu sırada gelen bildirimler beklemesin.
        val repo = TitleRepository()
        val progress = mutableMapOf<String, Int>()
        lockedTitleIds.forEach { titleId ->
            val current = runCatching { repo.getEpisodeProgress(titleId) }
                .getOrDefault(emptyList())
                .firstOrNull { it.profileId == profileId }
                ?.currentEpisode ?: 0
            progress[titleId] = current
        }
        mutex.withLock {
            val list = read()
            var changed = false
            val updated = list.map { entry ->
                val reached = entry.titleId?.let { progress[it] ?: 0 } ?: 0
                if (entry.locked && entry.episodeNumber != null && reached >= entry.episodeNumber) {
                    changed = true
                    entry.copy(locked = false)
                } else {
                    entry
                }
            }
            if (changed) write(updated)
        }
    }
}
