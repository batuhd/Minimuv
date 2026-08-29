package com.sinop.minimuv.core

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sinop.minimuv.R
import com.sinop.minimuv.data.TitleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object NotificationHelper {

    const val CHANNEL_ID = "minimuv_events"
    const val CHANNEL_ANNIVERSARY = "minimuv_anniversaries"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, "Partner Olayları", NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Partnerin bir şey izlediğinde veya eklediğinde" },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ANNIVERSARY, "Yıldönümleri", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Nostaljik hatırlatmalar" },
        )
    }

    fun maybeRequestPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= 33) {
            val has = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!has) {
                // Aktivite üzerinden istenir; ana akışta yapılır.
            }
        }
    }

    fun show(context: Context, title: String, text: String, id: Int, channel: String = CHANNEL_ID) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}

/**
 * Uygulama açıkken realtime olaylarını dinleyen watcher. Bildirim kararları
 * ortak PingNotifier / TitleTransitionTracker / AnniversaryChecker ile alınır —
 * böylece WorkManager worker'ı ile aynı olay iki kez bildirilmez.
 */
class PartnerEventWatcher(
    private val context: Context,
    private val profileId: String,
) {
    private val repo = TitleRepository()

    suspend fun init() {
        // Uygulama kapalıyken gelen yeni olaylar açılır açılmaz bildirilir
        PingNotifier.process(context, profileId)
        ScoreNotifier.process(context, profileId)
        NoteNotifier.process(context, profileId, NoteKind.TITLE)
        NoteNotifier.process(context, profileId, NoteKind.EPISODE)
        AnniversaryChecker.process(context)
    }

    fun observe(scope: CoroutineScope) {
        android.util.Log.d("MinimuvWatcher", "observe started (profile=$profileId)")
        scope.launch {
            RealtimeManager.events.collect { table ->
                android.util.Log.d("MinimuvWatcher", "event: $table")
                when (table) {
                    "titles" -> onTitlesChanged()
                    "episode_progress_per_profile" -> onProgressChanged()
                    "partner_pings" -> onNewPings()
                    "title_scores" -> onScoresChanged()
                    "title_notes" -> onTitleNotesChanged()
                    "episode_notes" -> onEpisodeNotesChanged()
                    "profiles" -> Unit // profil yenileme ekranlarda yapılır
                }
            }
        }
    }

    private suspend fun onNewPings() {
        PingNotifier.process(context, profileId)
    }

    private suspend fun onScoresChanged() {
        ScoreNotifier.process(context, profileId)
    }

    private suspend fun onTitleNotesChanged() {
        NoteNotifier.process(context, profileId, NoteKind.TITLE)
    }

    private suspend fun onEpisodeNotesChanged() {
        NoteNotifier.process(context, profileId, NoteKind.EPISODE)
    }

    private suspend fun onTitlesChanged() {
        try {
            val current = repo.getTitles()
            TitleTransitionTracker.diff(context, profileId, current).forEach { (title, text) ->
                notify(title, text)
            }
        } catch (_: Exception) {
            // çevrimdışı — sessizce geç
        }
    }

    private suspend fun onProgressChanged() {
        // Ayrı moddaki başlıklar için bölüm kilometre taşları (kalıcı durumla)
        MilestoneTracker.process(context, profileId)
    }

    private fun notify(title: String, text: String) {
        NotificationHelper.show(context, title, text, (title.hashCode() + System.currentTimeMillis()).toInt())
    }
}
