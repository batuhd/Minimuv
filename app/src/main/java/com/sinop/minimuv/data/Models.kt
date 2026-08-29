package com.sinop.minimuv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ContentType(val db: String, val label: String) {
    FILM("film", "Film"),
    DIZI("dizi", "Dizi"),
    ANIME("anime", "Anime");

    companion object {
        fun fromDb(value: String): ContentType =
            entries.firstOrNull { it.db == value } ?: FILM
    }
}

enum class WatchStatus(val db: String, val label: String) {
    WATCHING("Watching", "İzliyoruz"),
    PLAN("Plan to Watch", "Sırada"),
    COMPLETED("Completed", "Tamamlandı"),
    REWATCHING("Rewatching", "Yeniden"),
    PAUSED("Paused", "Duraklattık"),
    DROPPED("Dropped", "Bıraktık");

    companion object {
        fun fromDb(value: String): WatchStatus =
            entries.firstOrNull { it.db == value } ?: PLAN
    }
}

enum class WatchMode(val db: String, val label: String) {
    BIRLIKTE("birlikte", "Birlikte izliyoruz"),
    AYRI("ayri", "Ayrı ayrı izliyoruz");

    companion object {
        fun fromDb(value: String): WatchMode =
            entries.firstOrNull { it.db == value } ?: BIRLIKTE
    }
}

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val emoji: String? = null,
    @SerialName("avatar_color") val avatarColor: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class Title(
    val id: String,
    @SerialName("created_by_profile_id") val createdByProfileId: String? = null,
    val type: String,
    @SerialName("external_id") val externalId: String? = null,
    val title: String,
    @SerialName("poster_url") val posterUrl: String? = null,
    val overview: String? = null,
    val status: String = "Plan to Watch",
    val score: Double? = null,
    @SerialName("episode_progress") val episodeProgress: Int = 0,
    @SerialName("total_episodes") val totalEpisodes: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("finish_date") val finishDate: String? = null,
    @SerialName("total_rewatches") val totalRewatches: Int = 0,
    val notes: String? = null,
    val story: Double? = null,
    val characters: Double? = null,
    val visuals: Double? = null,
    val audio: Double? = null,
    val enjoyment: Double? = null,
    @SerialName("custom_lists") val customLists: List<String> = emptyList(),
    @SerialName("is_private") val isPrivate: Boolean = false,
    @SerialName("watch_mode") val watchMode: String = "birlikte",
    @SerialName("priority_order") val priorityOrder: Int? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
) {
    fun advancedScoreAverage(): Double? {
        val values = listOfNotNull(story, characters, visuals, audio, enjoyment)
        return if (values.isEmpty()) null else values.average()
    }
}

@Serializable
data class TitleScore(
    val id: String? = null,
    @SerialName("title_id") val titleId: String,
    @SerialName("profile_id") val profileId: String,
    val score: Double? = null,
    val story: Double? = null,
    val characters: Double? = null,
    val visuals: Double? = null,
    val audio: Double? = null,
    val enjoyment: Double? = null,
) {
    fun advancedAverage(): Double? {
        val values = listOfNotNull(story, characters, visuals, audio, enjoyment)
        return if (values.isEmpty()) null else values.average()
    }
}

@Serializable
data class PartnerPing(
    val id: String? = null,
    @SerialName("from_profile") val fromProfile: String,
    val message: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class EpisodeProgress(
    val id: String? = null,
    @SerialName("title_id") val titleId: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("current_episode") val currentEpisode: Int = 0,
)

@Serializable
data class EpisodeNote(
    val id: String? = null,
    @SerialName("title_id") val titleId: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("episode_number") val episodeNumber: Int,
    @SerialName("note_text") val noteText: String? = null,
    @SerialName("emoji_reaction") val emojiReaction: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Achievement(
    val id: String? = null,
    @SerialName("achievement_key") val achievementKey: String,
    @SerialName("unlocked_at") val unlockedAt: String? = null,
    @SerialName("progress_current") val progressCurrent: Int? = null,
    @SerialName("progress_target") val progressTarget: Int? = null,
)

@Serializable
data class WatchLog(
    val id: String? = null,
    @SerialName("title_id") val titleId: String,
    @SerialName("profile_id") val profileId: String,
    val date: String,
    @SerialName("episodes_watched") val episodesWatched: Int = 1,
)

// ── Başlığa yazılan tekil notlar (eski titles.notes'in yerine) ───────────

@Serializable
data class TitleNote(
    val id: String? = null,
    @SerialName("title_id") val titleId: String,
    @SerialName("profile_id") val profileId: String,
    @SerialName("note_text") val noteText: String,
    @SerialName("created_at") val createdAt: String? = null,
)

// ── Ekranlar arası taslak (henüz kaydedilmemiş yeni başlık) ──────────────

data class TitleDraft(
    val type: String,
    val externalId: String? = null,
    val title: String,
    val posterUrl: String? = null,
    val overview: String? = null,
    val totalEpisodes: Int? = null,
)
