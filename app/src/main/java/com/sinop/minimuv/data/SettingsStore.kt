package com.sinop.minimuv.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "minimuv_settings")

class SettingsStore(private val context: Context) {

    companion object {
        val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        val KEY_ANON_KEY = stringPreferencesKey("anon_key")
        val KEY_PROFILE_ID = stringPreferencesKey("profile_id")
        val KEY_ONBOARDING_DONE = stringPreferencesKey("onboarding_done")
        val KEY_THEME_ACCENT = stringPreferencesKey("theme_accent")
    }

    val supabaseUrl: Flow<String?> = context.dataStore.data.map { it[KEY_SUPABASE_URL] }
    val anonKey: Flow<String?> = context.dataStore.data.map { it[KEY_ANON_KEY] }
    val profileId: Flow<String?> = context.dataStore.data.map { it[KEY_PROFILE_ID] }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] == "true" }
    val themeAccent: Flow<String?> = context.dataStore.data.map { it[KEY_THEME_ACCENT] }

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

    suspend fun clearConnection() {
        context.dataStore.edit {
            it.remove(KEY_SUPABASE_URL)
            it.remove(KEY_ANON_KEY)
            it.remove(KEY_PROFILE_ID)
        }
    }
}
