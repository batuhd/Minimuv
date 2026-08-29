package com.sinop.minimuv.data

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.sinop.minimuv.core.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Cihazın FCM token'ını Supabase'e kaydeder (fcm_tokens tablosu).
 * Edge Function bu token'a bildirim gönderir. Token değişirse eski satırlar
 * zamanla geçersiz olur; Edge Function geçersiz tokenları kendisi temizler.
 */
class TokenRepository(private val context: Context) {

    suspend fun saveToken(token: String? = null) {
        val actual = token ?: fetchFcmToken() ?: return
        if (actual.isBlank()) return
        val settings = SettingsStore(context)
        val prefs = settings.rawPrefs.first()
        val url = settings.urlFrom(prefs) ?: return
        val key = settings.keyFrom(prefs) ?: return
        val profileId = settings.profileFrom(prefs)
        SupabaseProvider.configure(url, key)

        // Race-safe tek hamle: token unique olduğundan onNewToken ve MainApp
        // aynı anda çağırsa bile çakışmaz; varsa profilini günceller.
        runCatching {
            SupabaseProvider.client.postgrest.from("fcm_tokens").upsert(
                FcmToken(token = actual, profileId = profileId),
            ) {
                onConflict = "token"
            }
        }.onSuccess {
            android.util.Log.d("MinimuvFcm", "token kaydedildi (profile=$profileId)")
        }.onFailure {
            android.util.Log.e("MinimuvFcm", "token kaydedilemedi", it)
        }
    }

    private suspend fun fetchFcmToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                val result = if (task.isSuccessful) task.result else null
                if (result == null) {
                    android.util.Log.e("MinimuvFcm", "FCM token alınamadı", task.exception)
                }
                if (cont.isActive) cont.resume(result)
            }
    }
}
