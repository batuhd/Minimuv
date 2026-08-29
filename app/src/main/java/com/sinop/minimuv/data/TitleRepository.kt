package com.sinop.minimuv.data

import com.sinop.minimuv.core.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonNull
import com.sinop.minimuv.data.PartnerPing

class TitleRepository {

    suspend fun getTitles(): List<Title> =
        SupabaseProvider.client.postgrest.from("titles")
            .select { order("created_at", Order.ASCENDING) }
            .decodeList<Title>()

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
                        is Boolean -> set(key, value)
                        is List<*> -> set(key, value.filterIsInstance<String>())
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
        SupabaseProvider.client.postgrest.from("title_scores")
            .select()
            .decodeList<TitleScore>()

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
            SupabaseProvider.client.postgrest.from("title_scores")
                .insert(score.copy(id = null))
        } else {
            val rowId = existing.first().id!!
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
                filter { eq("id", rowId) }
            }
        }
    }

    /** İki kişinin puan ortalamasını titles.score'a yazar. */
    suspend fun refreshCoupleScore(titleId: String) {
        val scores = getTitleScores(titleId).mapNotNull { it.score }
        val avg = if (scores.isEmpty()) null else scores.average()
        SupabaseProvider.client.postgrest.from("titles").update(
            {
                if (avg == null) set("score", JsonNull) else set("score", round1(avg))
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

    suspend fun setEpisodeProgress(titleId: String, profileId: String, episode: Int) {
        val existing = SupabaseProvider.client.postgrest.from("episode_progress_per_profile")
            .select {
                filter {
                    eq("title_id", titleId)
                    eq("profile_id", profileId)
                }
            }
            .decodeList<EpisodeProgress>()
        if (existing.isEmpty()) {
            SupabaseProvider.client.postgrest.from("episode_progress_per_profile")
                .insert(EpisodeProgress(titleId = titleId, profileId = profileId, currentEpisode = episode))
        } else {
            SupabaseProvider.client.postgrest.from("episode_progress_per_profile")
                .update({ set("current_episode", episode) }) {
                    filter {
                        eq("title_id", titleId)
                        eq("profile_id", profileId)
                    }
                }
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
        SupabaseProvider.client.postgrest.from("episode_notes")
            .select()
            .decodeList<EpisodeNote>()

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
        SupabaseProvider.client.postgrest.from("title_notes")
            .select()
            .decodeList<TitleNote>()

    // ── İzleme günlüğü ───────────────────────────────────────────────────

    /** İzleme günlüğünü sayfalar halinde çeker — PostgREST varsayılan 1000 satır
     *  limiti nedeniyle eski kayıtların sessizce kaybolmasını engeller. */
    suspend fun getWatchLog(): List<WatchLog> {
        val all = mutableListOf<WatchLog>()
        val pageSize = 1000L
        var offset = 0L
        while (true) {
            val page = SupabaseProvider.client.postgrest.from("watch_log")
                .select {
                    order("date", Order.ASCENDING)
                    range(offset, offset + pageSize - 1)
                }
                .decodeList<WatchLog>()
            all += page
            if (page.size < pageSize) break
            offset += pageSize
            if (offset > 20_000) break // güvenlik sınırı
        }
        return all
    }

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
        val exists = SupabaseProvider.client.postgrest.from("achievements")
            .select { filter { eq("achievement_key", key) } }
            .decodeList<Achievement>()
        if (exists.isEmpty()) {
            SupabaseProvider.client.postgrest.from("achievements")
                .insert(
                    Achievement(
                        achievementKey = key,
                        progressCurrent = current,
                        progressTarget = target,
                    )
                )
        }
    }
}
