package com.sinop.minimuv.core

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Uygulama kapalıyken bile bildirimlerin çalışmasını sağlayan periyodik kontrol.
 * Kalıcı ön plan servisi ve zorunlu bildirimi YOKTUR; Android'in kendi
 * zamanlayıcısı (WorkManager) uygulama süreci ölü olsa da bu işi uyandırır.
 */
object BackgroundNotifier {

    private const val UNIQUE_WORK = "minimuv_background_notifications"
    const val INTERVAL_MINUTES = 15L

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(
            INTERVAL_MINUTES, TimeUnit.MINUTES,
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }.onFailure { Log.w("BackgroundNotifier", "zamanlama hatası", it) }
    }
}

class NotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        return try {
            val settings = SettingsStore(context)
            val prefs = settings.rawPrefs.first()
            val url = settings.urlFrom(prefs)
            val key = settings.keyFrom(prefs)
            val profileId = settings.profileFrom(prefs)
            if (url.isNullOrBlank() || key.isNullOrBlank() || profileId.isNullOrBlank()) {
                Log.d("NotificationWorker", "kurulum eksik — atlanıyor")
                return Result.failure()
            }
            SupabaseProvider.configure(url, key)
            NotificationHelper.ensureChannels(context)

            val repo = TitleRepository()
            PingNotifier.process(context, profileId)
            TitleTransitionTracker.diff(context, profileId, repo.getTitles()).forEach { (title, text) ->
                NotificationHelper.show(context, title, text, (title.hashCode() + System.currentTimeMillis()).toInt())
            }
            MilestoneTracker.process(context, profileId)
            ScoreNotifier.process(context, profileId)
            NoteNotifier.process(context, profileId, NoteKind.TITLE)
            NoteNotifier.process(context, profileId, NoteKind.EPISODE)
            AnniversaryChecker.process(context)
            Log.d("NotificationWorker", "kontrol tamamlandı")
            Result.success()
        } catch (e: Exception) {
            Log.w("NotificationWorker", "kontrol hatası (retry)", e)
            Result.retry()
        }
    }
}
