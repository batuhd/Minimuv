package com.sinop.minimuv.core

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sinop.minimuv.MainActivity
import com.sinop.minimuv.R
import com.sinop.minimuv.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Uygulama kapalıyken de partner bildirimlerini alabilmek için
 * realtime bağlantısını canlı tutan ön plan servisi.
 */
class RealtimeService : Service() {

    companion object {
        private const val NOTIF_ID = 1001
        const val CHANNEL_SYNC = "minimuv_sync"

        fun start(context: Context) {
            val intent = Intent(context, RealtimeService::class.java)
            runCatching { context.startForegroundService(intent) }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var started = false

    private var watcher: PartnerEventWatcher? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = android.app.NotificationChannel(
            CHANNEL_SYNC, "Minimuv Senkron", android.app.NotificationManager.IMPORTANCE_LOW,
        ).apply { description = "Partner bildirimleri için bağlantıyı canlı tutar" }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.createNotificationChannel(channel)

        val launchIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Minimuv 🔔")
            .setContentText("Partner bildirimleri açık")
            .setContentIntent(pi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()

        startForeground(NOTIF_ID, notification)

        if (!started) {
            started = true
            scope.launch {
                runCatching { startInternal() }
                    .onFailure {
                        android.util.Log.e("RealtimeService", "startInternal failed", it)
                        started = false
                        stopSelf()
                    }
            }
        }
        return START_STICKY
    }

    private suspend fun startInternal() {
        val settings = SettingsStore(applicationContext)
        val prefs = settings.rawPrefs.first()
        val url = settings.urlFrom(prefs)
        val key = settings.keyFrom(prefs)
        val profileId = settings.profileFrom(prefs)
        if (url.isNullOrBlank() || key.isNullOrBlank() || profileId.isNullOrBlank()) {
            stopSelf()
            return
        }
        SupabaseProvider.configure(url, key)
        RealtimeManager.start()
        val w = PartnerEventWatcher(applicationContext, profileId)
        w.init()
        w.observe(scope)
        watcher = w
        android.util.Log.d("RealtimeService", "realtime service aktif (profile=$profileId)")
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("RealtimeService", "servis kapatıldı")
    }
}
