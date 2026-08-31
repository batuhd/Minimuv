package com.sinop.minimuv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "minimuv_settings")

class SettingsStore(private val context: Context) {

    companion object {
        val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        val KEY_ANON_KEY = stringPreferencesKey("anon_key")
        val KEY_PROFILE_ID = stringPreferencesKey("profile_id")
        val KEY_ONBOARDING_DONE = stringPreferencesKey("onboarding_done")
        val KEY_THEME_ACCENT = stringPreferencesKey("theme_accent")
        val KEY_LAST_PING_AT = stringPreferencesKey("last_ping_at")
        val KEY_TITLES_SNAPSHOT = stringPreferencesKey("titles_snapshot")
        val KEY_LAST_ANNIV_DATE = stringPreferencesKey("last_anniv_date")
        val KEY_MILESTONES_SNAPSHOT = stringPreferencesKey("milestones_snapshot")
        val KEY_SCORE_SNAPSHOT = stringPreferencesKey("score_snapshot")
        val KEY_LAST_TITLE_NOTE_AT = stringPreferencesKey("last_title_note_at")
        val KEY_LAST_EPISODE_NOTE_AT = stringPreferencesKey("last_episode_note_at")
        val KEY_LIST_VIEW = stringPreferencesKey("list_view")
        val KEY_SEARCH_LANG = stringPreferencesKey("search_lang")
    }

    val supabaseUrl: Flow<String?> = context.dataStore.data.map { it[KEY_SUPABASE_URL] }
    val anonKey: Flow<String?> = context.dataStore.data.map { it[KEY_ANON_KEY] }
    val profileId: Flow<String?> = context.dataStore.data.map { it[KEY_PROFILE_ID] }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] == "true" }
    val themeAccent: Flow<String?> = context.dataStore.data.map { it[KEY_THEME_ACCENT] }
    val listView: Flow<String?> = context.dataStore.data.map { it[KEY_LIST_VIEW] }
    val searchLang: Flow<String?> = context.dataStore.data.map { it[KEY_SEARCH_LANG] }

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

    suspend fun clearProfile() {
        context.dataStore.edit { it.remove(KEY_PROFILE_ID) }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = "true" }
    }

    suspend fun setThemeAccent(value: String) {
        context.dataStore.edit { it[KEY_THEME_ACCENT] = value }
    }

    suspend fun saveListView(value: String) {
        context.dataStore.edit { it[KEY_LIST_VIEW] = value }
    }

    suspend fun saveSearchLang(value: String) {
        context.dataStore.edit { it[KEY_SEARCH_LANG] = value }
    }

    suspend fun clearConnection() {
        context.dataStore.edit {
            it.remove(KEY_SUPABASE_URL)
            it.remove(KEY_ANON_KEY)
            it.remove(KEY_PROFILE_ID)
        }
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
