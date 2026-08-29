package com.sinop.minimuv.core

import android.content.Context
import com.sinop.minimuv.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ön plan servisi olmadan partner olaylarını dinleyen uygulama kapsamlı çalıştırıcı.
 * Uygulama süreci yaşadığı sürece realtime bağlantısı ve bildirimler aktiftir;
 * kalıcı servis bildirimi gösterilmez.
 */
object PartnerEventsRuntime {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var started = false

    /** Realtime watcher'ı aktif mi? (FCM mesaj yolu çifte bildirim yapmasın.) */
    fun isRunning(): Boolean = started

    fun start(context: Context) {
        if (started) return
        started = true
        scope.launch {
            runCatching {
                val appContext = context.applicationContext
                val settings = SettingsStore(appContext)
                val prefs = settings.rawPrefs.first()
                val url = settings.urlFrom(prefs)
                val key = settings.keyFrom(prefs)
                val profileId = settings.profileFrom(prefs)
                if (url.isNullOrBlank() || key.isNullOrBlank() || profileId.isNullOrBlank()) {
                    started = false
                    return@launch
                }
                SupabaseProvider.configure(url, key)
                RealtimeManager.start()
                val watcher = PartnerEventWatcher(appContext, profileId)
                watcher.init()
                watcher.observe(scope)
                android.util.Log.d("PartnerEventsRuntime", "aktif (profile=$profileId)")
            }.onFailure {
                started = false
                android.util.Log.e("PartnerEventsRuntime", "başlatılamadı", it)
            }
        }
    }
}
