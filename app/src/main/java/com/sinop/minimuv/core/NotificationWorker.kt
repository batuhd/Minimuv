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

    fun scheduleTestNotification(context: Context, delaySeconds: Long = 5L) {
        val request = androidx.work.OneTimeWorkRequestBuilder<TestNotificationWorker>()
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        runCatching {
            WorkManager.getInstance(context).enqueue(request)
        }.onFailure { Log.w("BackgroundNotifier", "test bildirim zamanlama hatası", it) }
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
            TitleTransitionTracker.diff(context, profileId, repo.getTitlesLite()).forEach { n ->
                NotificationHelper.show(
                    context,
                    n.title,
                    n.text,
                    (n.title.hashCode() + System.currentTimeMillis()).toInt(),
                    meta = n.meta,
                )
            }
            MilestoneTracker.process(context, profileId)
            ScoreNotifier.process(context, profileId)
            NoteNotifier.process(context, profileId, NoteKind.TITLE)
            NoteNotifier.process(context, profileId, NoteKind.EPISODE)
            AnniversaryChecker.process(context)
            Log.d("NotificationWorker", "kontrol tamamlandı")
            Result.success()
        } catch (e: kotlinx.serialization.SerializationException) {
            // Kalıcı veri hatası: sonsuz retry yerine başarısız bitir (BUG-035).
            Log.e("NotificationWorker", "kalıcı veri hatası", e)
            Result.failure()
        } catch (e: IllegalArgumentException) {
            Log.e("NotificationWorker", "kalıcı argüman hatası", e)
            Result.failure()
        } catch (e: java.io.IOException) {
            Log.w("NotificationWorker", "geçici ağ hatası (retry)", e)
            Result.retry()
        } catch (e: Exception) {
            Log.w("NotificationWorker", "kontrol hatası (retry)", e)
            Result.retry()
        }
    }
}

class TestNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        NotificationHelper.ensureChannels(context)
        NotificationHelper.show(
            context,
            "💌 Minimuv Test Bildirimi",
            "Uygulama kapalıyken bile bildirimler sorunsuz çalışıyor! 🎉",
            id = (System.currentTimeMillis() % 100000).toInt(),
        )
        return Result.success()
    }
}
