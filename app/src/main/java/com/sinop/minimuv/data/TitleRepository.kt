package com.sinop.minimuv.data

import com.sinop.minimuv.core.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.Serializable
import com.sinop.minimuv.data.PartnerPing

@Serializable
private data class ScoreProbe(val score: Double? = null)

class TitleRepository {

    suspend fun getTitles(): List<Title> =
        fetchAllPaged("titles", "created_at")

    /** Liste ekranı için gerekli kolonları çeker — overview/notlar gibi ağır
     *  alanlar taşınmaz; JSON payload ve decode süresi belirgin azalır. */
    suspend fun getTitlesLite(): List<Title> =
        fetchAllPaged(
            table = "titles",
            orderColumn = "created_at",
            columns = listOf(
                "id",
                "created_by_profile_id",
                "updated_by_profile_id",
                "type",
                "external_id",
                "title",
                "title_en",
                "poster_url",
                "status",
                "score",
                "episode_progress",
                "total_episodes",
                "start_date",
                "finish_date",
                "total_rewatches",
                "priority_order",
                "watch_order",
                "watch_mode",
                "is_favorite",
                "created_at",
                "updated_at",
                "notes",
            ),
        )

    suspend fun getTitle(id: String): Title? =
        SupabaseProvider.client.postgrest.from("titles")
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<Title>()

    suspend fun insert(title: Title) {
        SupabaseProvider.client.postgrest.from("titles").insert(title)
    }

    suspend fun update(id: String, changes: Map<String, Any?>) {
        if (changes.isEmpty()) return
        SupabaseProvider.client.postgrest.from("titles").update(
            {
                changes.forEach { (key, value) ->
                    when (value) {
                        null -> set(key, JsonNull)
                        is String -> set(key, value)
                        is Double -> set(key, value)
                        is Int -> set(key, value)
                        is Float -> set(key, value.toDouble())
                        is Long -> set(key, value)
                        is Boolean -> set(key, value)
                        is List<*> -> set(key, value.filterIsInstance<String>())
                        else -> android.util.Log.w(
                            "TitleRepo",
                            "Desteklenmeyen tip: $key (${value::class.simpleName})",
                        )
                    }
                }
            }
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun updatePriority(id: String, priorityOrder: Int) {
        SupabaseProvider.client.postgrest.from("titles").update(
            {
                set("priority_order", priorityOrder)
            }
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun updateWatchOrder(id: String, order: Int) {
        SupabaseProvider.client.postgrest.from("titles").update(
            {
                set("watch_order", order)
            }
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun delete(id: String) {
        SupabaseProvider.client.postgrest.from("titles").delete {
            filter { eq("id", id) }
        }
    }

    // ── Kişi bazlı puanlar ───────────────────────────────────────────────

    suspend fun getTitleScores(titleId: String): List<TitleScore> =
        SupabaseProvider.client.postgrest.from("title_scores")
            .select { filter { eq("title_id", titleId) } }
            .decodeList<TitleScore>()

    /** Tüm kişi-bazlı puanlar (rozet istatistikleri için). */
    suspend fun getAllTitleScores(): List<TitleScore> =
        fetchAllPaged("title_scores", "id")

    suspend fun upsertTitleScore(score: TitleScore) {
        val existing = SupabaseProvider.client.postgrest.from("title_scores")
            .select {
                filter {
                    eq("title_id", score.titleId)
                    eq("profile_id", score.profileId)
                }
            }
            .decodeList<TitleScore>()
        if (existing.isEmpty()) {
            // İki cihaz aynı anda insert ederse unique ihlali olur; kaybetmeden
            // güncellemeye düşerek yarışı kapat (BUG-045).
            runCatching {
                SupabaseProvider.client.postgrest.from("title_scores")
                    .insert(score.copy(id = null))
            }.onFailure {
                updateScoreRow(score)
            }
        } else {
            updateScoreRow(score)
        }
    }

    /** Null alanların da temizlenmesi için açıkça JsonNull yazar (DS-BUG-02). */
    private suspend fun updateScoreRow(score: TitleScore) {
        SupabaseProvider.client.postgrest.from("title_scores").update(
            {
                if (score.score == null) set("score", JsonNull) else set("score", score.score!!)
                if (score.story == null) set("story", JsonNull) else set("story", score.story!!)
                if (score.characters == null) set("characters", JsonNull) else set("characters", score.characters!!)
                if (score.visuals == null) set("visuals", JsonNull) else set("visuals", score.visuals!!)
                if (score.audio == null) set("audio", JsonNull) else set("audio", score.audio!!)
                if (score.enjoyment == null) set("enjoyment", JsonNull) else set("enjoyment", score.enjoyment!!)
            }
        ) {
            filter {
                eq("title_id", score.titleId)
                eq("profile_id", score.profileId)
            }
        }
    }

    /** İki kişinin puan ortalamasını titles.score'a yazar.
     *  Değer zaten aynıysa yazmaz — titles UPDATE'inin realtime olay üretip
     *  DetailViewModel.reload → refreshCoupleScore döngüsünü tetiklemesini engeller. */
    suspend fun refreshCoupleScore(titleId: String) {
        val scores = getTitleScores(titleId).mapNotNull { it.score }
        val avg = if (scores.isEmpty()) null else round1(scores.average())
        val current = SupabaseProvider.client.postgrest.from("titles")
            .select(Columns.list("score")) { filter { eq("id", titleId) } }
            .decodeSingleOrNull<ScoreProbe>()
            ?.score
        val changed = when {
            current == null && avg == null -> false
            current == null || avg == null -> true
            else -> kotlin.math.abs(current - avg) >= 0.05
        }
        if (!changed) return
        SupabaseProvider.client.postgrest.from("titles").update(
            {
                if (avg == null) set("score", JsonNull) else set("score", avg)
            }
        ) {
            filter { eq("id", titleId) }
        }
    }

    private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0

    // ── Bölüm ilerlemesi (ayrı mod) ──────────────────────────────────────

    suspend fun getEpisodeProgress(titleId: String): List<EpisodeProgress> =
        SupabaseProvider.client.postgrest.from("episode_progress_per_profile")
            .select { filter { eq("title_id", titleId) } }
            .decodeList<EpisodeProgress>()

    suspend fun getAllEpisodeProgress(): List<EpisodeProgress> =
        fetchAllPaged("episode_progress_per_profile", "id")

    suspend fun setEpisodeProgress(titleId: String, profileId: String, episode: Int) {
        // Atomik upsert: select-then-insert yarışını kapatır (BUG-045).
        SupabaseProvider.client.postgrest.from("episode_progress_per_profile")
            .upsert(
                EpisodeProgress(titleId = titleId, profileId = profileId, currentEpisode = episode),
            ) {
                onConflict = "title_id,profile_id"
            }
    }

    // ── Bölüm notları ────────────────────────────────────────────────────

    suspend fun getEpisodeNotes(titleId: String): List<EpisodeNote> =
        SupabaseProvider.client.postgrest.from("episode_notes")
            .select {
                filter { eq("title_id", titleId) }
                order("episode_number", Order.ASCENDING)
                order("created_at", Order.ASCENDING)
            }
            .decodeList<EpisodeNote>()

    /** Tüm bölüm notları (bildirim dedup'u için). */
    suspend fun getAllEpisodeNotes(): List<EpisodeNote> =
        fetchAllPaged("episode_notes", "created_at")

    suspend fun insertEpisodeNote(note: EpisodeNote) {
        SupabaseProvider.client.postgrest.from("episode_notes")
            .insert(note)
    }

    suspend fun deleteEpisodeNote(id: String) {
        SupabaseProvider.client.postgrest.from("episode_notes").delete {
            filter { eq("id", id) }
        }
    }

    // ── Başlık notları (tek tek) ─────────────────────────────────────────

    suspend fun getTitleNotes(titleId: String): List<TitleNote> =
        SupabaseProvider.client.postgrest.from("title_notes")
            .select {
                filter { eq("title_id", titleId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<TitleNote>()

    suspend fun insertTitleNote(note: TitleNote) {
        SupabaseProvider.client.postgrest.from("title_notes").insert(note)
    }

    suspend fun updateTitleNote(id: String, text: String) {
        SupabaseProvider.client.postgrest.from("title_notes").update(
            { set("note_text", text) }
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteTitleNote(id: String) {
        SupabaseProvider.client.postgrest.from("title_notes").delete {
            filter { eq("id", id) }
        }
    }

    suspend fun getAllTitleNotes(): List<TitleNote> =
        fetchAllPaged("title_notes", "created_at")

    // ── İzleme günlüğü ───────────────────────────────────────────────────

    suspend fun getWatchLog(): List<WatchLog> =
        fetchAllPaged("watch_log", "date")

    suspend fun insertWatchLog(entry: WatchLog) {
        SupabaseProvider.client.postgrest.from("watch_log").insert(entry)
    }

    suspend fun getWatchLogForTitle(titleId: String): List<WatchLog> =
        SupabaseProvider.client.postgrest.from("watch_log")
            .select { filter { eq("title_id", titleId) } }
            .decodeList<WatchLog>()

    // ── Partnere gizli mesaj ─────────────────────────────────────────────

    suspend fun sendPartnerPing(fromProfileId: String, message: String) {
        SupabaseProvider.client.postgrest.from("partner_pings")
            .insert(PartnerPing(fromProfile = fromProfileId, message = message))
    }

    suspend fun getRecentPings(limit: Int = 10): List<PartnerPing> =
        SupabaseProvider.client.postgrest.from("partner_pings")
            .select {
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<PartnerPing>()

    // ── Başarımlar ───────────────────────────────────────────────────────

    suspend fun getAchievements(): List<Achievement> =
        SupabaseProvider.client.postgrest.from("achievements")
            .select()
            .decodeList<Achievement>()

    suspend fun unlockAchievement(key: String, current: Int?, target: Int?) {
        // Atomik: select-then-insert yarışında unique ihlaline düşmemek için
        // çakışmayı yok sayan upsert (BUG-045).
        SupabaseProvider.client.postgrest.from("achievements")
            .upsert(
                Achievement(
                    achievementKey = key,
                    progressCurrent = current,
                    progressTarget = target,
                ),
            ) {
                onConflict = "achievement_key"
                ignoreDuplicates = true
            }
    }

    // ── Ortak sayfalama ──────────────────────────────────────────────────

    /** PostgREST varsayılan 1000 satır limitini aşan tabloları sayfalayarak çeker.
     *  `id` ikincil sıralaması eşit sıralama anahtarlarında sayfa kaymasını
     *  (kayıt atlanması/tekrarı) önler (BUG-014). */
    private suspend inline fun <reified T : Any> fetchAllPaged(
        table: String,
        orderColumn: String,
        columns: List<String>? = null,
        pageSize: Long = 1000L,
    ): List<T> {
        val all = mutableListOf<T>()
        var offset = 0L
        while (true) {
            val request = if (columns == null) {
                SupabaseProvider.client.postgrest.from(table).select {
                    order(orderColumn, Order.ASCENDING)
                    if (orderColumn != "id") order("id", Order.ASCENDING)
                    range(offset, offset + pageSize - 1)
                }
            } else {
                SupabaseProvider.client.postgrest.from(table)
                    .select(Columns.list(*columns.toTypedArray())) {
                        order(orderColumn, Order.ASCENDING)
                        if (orderColumn != "id") order("id", Order.ASCENDING)
                        range(offset, offset + pageSize - 1)
                    }
            }
            val page = request.decodeList<T>()
            all += page
            if (page.size < pageSize) break
            offset += pageSize
            if (offset > 20_000) break // güvenlik sınırı
        }
        return all
    }
}
