package com.sinop.minimuv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val Context.dataStore by preferencesDataStore(name = "minimuv_settings")

class SettingsStore(private val context: Context) {

    companion object {
        val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        val KEY_ANON_KEY = stringPreferencesKey("anon_key")
        val KEY_PROFILE_ID = stringPreferencesKey("profile_id")
        val KEY_ONBOARDING_DONE = stringPreferencesKey("onboarding_done")
        // Tema renkleri artık profillerden gelir; bu anahtarlar yalnızca açılışta
        // renk flaş etmemesi için son bilinen değerleri tutar.
        val KEY_ACCENT_ME = stringPreferencesKey("accent_me")
        val KEY_ACCENT_PARTNER = stringPreferencesKey("accent_partner")
        val KEY_LAST_PING_AT = stringPreferencesKey("last_ping_at")
        val KEY_TITLES_SNAPSHOT = stringPreferencesKey("titles_snapshot")
        val KEY_LAST_ANNIV_DATE = stringPreferencesKey("last_anniv_date")
        val KEY_MILESTONES_SNAPSHOT = stringPreferencesKey("milestones_snapshot")
        // "titleId|profileId" -> son görülen bölüm. Hangi tarafın ilerlediğini
        // ayırt etmek için: kendi eklediğimiz bölüm bildirim üretmesin.
        val KEY_MILESTONE_PROGRESS_SNAPSHOT = stringPreferencesKey("milestone_progress_snapshot")
        val KEY_SCORE_SNAPSHOT = stringPreferencesKey("score_snapshot")
        val KEY_LAST_TITLE_NOTE_AT = stringPreferencesKey("last_title_note_at")
        val KEY_LAST_EPISODE_NOTE_AT = stringPreferencesKey("last_episode_note_at")
        val KEY_LIST_VIEW = stringPreferencesKey("list_view")
        val KEY_SEARCH_LANG = stringPreferencesKey("search_lang")
        val KEY_DISPLAY_LANG = stringPreferencesKey("display_lang")
        // Çift ayarları (DB'deki couple_settings) için son bilinen değer.
        val KEY_TOGETHER_SINCE = stringPreferencesKey("together_since")
        // Son bilinen profil listesi (JSON) — soğuk açılışta avatar/isim anında.
        val KEY_PROFILES_SNAPSHOT = stringPreferencesKey("profiles_snapshot")
    }

    private val snapshotJson = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val supabaseUrl: Flow<String?> = context.dataStore.data.map { it[KEY_SUPABASE_URL] }
    val anonKey: Flow<String?> = context.dataStore.data.map { it[KEY_ANON_KEY] }
    val profileId: Flow<String?> = context.dataStore.data.map { it[KEY_PROFILE_ID] }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] == "true" }
    val accentMe: Flow<String?> = context.dataStore.data.map { it[KEY_ACCENT_ME] }
    val accentPartner: Flow<String?> = context.dataStore.data.map { it[KEY_ACCENT_PARTNER] }
    val listView: Flow<String?> = context.dataStore.data.map { it[KEY_LIST_VIEW] }
    val searchLang: Flow<String?> = context.dataStore.data.map { it[KEY_SEARCH_LANG] }
    val displayLang: Flow<String?> = context.dataStore.data.map { it[KEY_DISPLAY_LANG] }
    val togetherSince: Flow<String?> = context.dataStore.data.map { it[KEY_TOGETHER_SINCE] }

    /** null = henüz yüklenmedi; yüklendiyse içindeki değerler güvenilirdir. */
    val rawPrefs: Flow<androidx.datastore.preferences.core.Preferences> = context.dataStore.data

    fun urlFrom(prefs: androidx.datastore.preferences.core.Preferences?): String? =
        prefs?.get(KEY_SUPABASE_URL)

    fun keyFrom(prefs: androidx.datastore.preferences.core.Preferences?): String? =
        prefs?.get(KEY_ANON_KEY)

    fun profileFrom(prefs: androidx.datastore.preferences.core.Preferences?): String? =
        prefs?.get(KEY_PROFILE_ID)

    suspend fun saveConnection(url: String, key: String) {
        context.dataStore.edit {
            it[KEY_SUPABASE_URL] = url.trim().trimEnd('/')
            it[KEY_ANON_KEY] = key.trim()
        }
    }

    suspend fun saveProfile(profileId: String) {
        context.dataStore.edit { it[KEY_PROFILE_ID] = profileId }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = "true" }
    }

    /**
     * Profil listesinden kendi ve partner renklerini önbelleğe yazar.
     * Değişiklik yoksa DataStore'a dokunmaz (gereksiz tema emisyonu olmasın).
     */
    suspend fun cacheAccentColors(profiles: List<Profile>) {
        val prefs = context.dataStore.data.first()
        val myId = prefs[KEY_PROFILE_ID]
        val me = profiles.firstOrNull { it.id == myId }?.avatarColor
        val partner = profiles.firstOrNull { it.id != myId }?.avatarColor
        if (me == prefs[KEY_ACCENT_ME] && partner == prefs[KEY_ACCENT_PARTNER]) return
        context.dataStore.edit {
            if (me != null) it[KEY_ACCENT_ME] = me else it.remove(KEY_ACCENT_ME)
            if (partner != null) it[KEY_ACCENT_PARTNER] = partner else it.remove(KEY_ACCENT_PARTNER)
        }
    }

    suspend fun saveListView(value: String) {
        context.dataStore.edit { it[KEY_LIST_VIEW] = value }
    }

    suspend fun saveSearchLang(value: String) {
        context.dataStore.edit { it[KEY_SEARCH_LANG] = value }
    }

    suspend fun saveDisplayLang(value: String) {
        context.dataStore.edit { it[KEY_DISPLAY_LANG] = value }
    }

    suspend fun saveTogetherSince(value: String?) {
        context.dataStore.edit {
            if (value == null) it.remove(KEY_TOGETHER_SINCE) else it[KEY_TOGETHER_SINCE] = value
        }
    }

    /** DB'den gelen değeri önbelleğe yazar; değişiklik yoksa DataStore'a dokunmaz. */
    suspend fun cacheTogetherSince(value: String?) {
        if (context.dataStore.data.first()[KEY_TOGETHER_SINCE] == value) return
        saveTogetherSince(value)
    }

    /** Profil listesini (isim/emoji/renk/avatar URL) soğuk açılış için saklar. */
    suspend fun cacheProfilesSnapshot(profiles: List<Profile>) {
        if (profiles.isEmpty()) return
        val encoded = runCatching { snapshotJson.encodeToString(profiles) }.getOrNull() ?: return
        if (context.dataStore.data.first()[KEY_PROFILES_SNAPSHOT] == encoded) return
        context.dataStore.edit { it[KEY_PROFILES_SNAPSHOT] = encoded }
    }

    suspend fun readProfilesSnapshot(): List<Profile> {
        val raw = context.dataStore.data.first()[KEY_PROFILES_SNAPSHOT] ?: return emptyList()
        return runCatching { snapshotJson.decodeFromString<List<Profile>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun clearProfile() {
        context.dataStore.edit { it.remove(KEY_PROFILE_ID) }
    }

    suspend fun clearConnection() {
        context.dataStore.edit {
            it.remove(KEY_SUPABASE_URL)
            it.remove(KEY_ANON_KEY)
            it.remove(KEY_PROFILE_ID)
            it.remove(KEY_TOGETHER_SINCE)
            it.remove(KEY_PROFILES_SNAPSHOT)
        }
        AppCache.clear()
    }

    // ── Arka plan bildirim durumu ─────────────────────────────────────────

    suspend fun lastPingAt(): String? = context.dataStore.data.first()[KEY_LAST_PING_AT]

    suspend fun saveLastPingAt(value: String) {
        context.dataStore.edit { it[KEY_LAST_PING_AT] = value }
    }

    suspend fun titlesSnapshot(): String? = context.dataStore.data.first()[KEY_TITLES_SNAPSHOT]

    suspend fun saveTitlesSnapshot(value: String) {
        context.dataStore.edit { it[KEY_TITLES_SNAPSHOT] = value }
    }

    suspend fun lastAnnivDate(): String? = context.dataStore.data.first()[KEY_LAST_ANNIV_DATE]

    suspend fun saveLastAnnivDate(value: String) {
        context.dataStore.edit { it[KEY_LAST_ANNIV_DATE] = value }
    }

    suspend fun milestonesSnapshot(): String? = context.dataStore.data.first()[KEY_MILESTONES_SNAPSHOT]

    suspend fun saveMilestonesSnapshot(value: String) {
        context.dataStore.edit { it[KEY_MILESTONES_SNAPSHOT] = value }
    }

    suspend fun milestoneProgressSnapshot(): String? =
        context.dataStore.data.first()[KEY_MILESTONE_PROGRESS_SNAPSHOT]

    suspend fun saveMilestoneProgressSnapshot(value: String) {
        context.dataStore.edit { it[KEY_MILESTONE_PROGRESS_SNAPSHOT] = value }
    }

    suspend fun scoreSnapshot(): String? = context.dataStore.data.first()[KEY_SCORE_SNAPSHOT]

    suspend fun saveScoreSnapshot(value: String) {
        context.dataStore.edit { it[KEY_SCORE_SNAPSHOT] = value }
    }

    suspend fun lastTitleNoteAt(): String? = context.dataStore.data.first()[KEY_LAST_TITLE_NOTE_AT]

    suspend fun saveLastTitleNoteAt(value: String) {
        context.dataStore.edit { it[KEY_LAST_TITLE_NOTE_AT] = value }
    }

    suspend fun lastEpisodeNoteAt(): String? = context.dataStore.data.first()[KEY_LAST_EPISODE_NOTE_AT]

    suspend fun saveLastEpisodeNoteAt(value: String) {
        context.dataStore.edit { it[KEY_LAST_EPISODE_NOTE_AT] = value }
    }
}
