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

    suspend fun insertEpisodeNote(note: EpisodeNote) {
        SupabaseProvider.client.postgrest.from("episode_notes")
            .insert(note)
    }

    suspend fun deleteEpisodeNote(id: String) {
        SupabaseProvider.client.postgrest.from("episode_notes").delete {
            filter { eq("id", id) }
        }
    }

    // ── İzleme günlüğü ───────────────────────────────────────────────────

    suspend fun getWatchLog(): List<WatchLog> =
        SupabaseProvider.client.postgrest.from("watch_log")
            .select()
            .decodeList<WatchLog>()

    suspend fun insertWatchLog(entry: WatchLog) {
        SupabaseProvider.client.postgrest.from("watch_log").insert(entry)
    }

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
