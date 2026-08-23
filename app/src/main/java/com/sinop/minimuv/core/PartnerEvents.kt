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
import com.sinop.minimuv.data.EpisodeProgress
import com.sinop.minimuv.data.Title
import com.sinop.minimuv.data.TitleRepository
import com.sinop.minimuv.data.WatchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period

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

class PartnerEventWatcher(
    private val context: Context,
    private val profileId: String,
) {
    private val repo = TitleRepository()
    private val profileRepo = com.sinop.minimuv.data.ProfileRepository()
    private var lastTitles: Map<String, Title> = emptyMap()
    private var milestoneNotified: MutableMap<String, Int> = mutableMapOf()
    private val seenPingIds = mutableSetOf<String>()
    private val startedAt = java.time.Instant.now().minusSeconds(2)

    suspend fun init() {
        lastTitles = repo.getTitles().associateBy { it.id }
        // Açılıştan önceki pingleri "görülmüş" say; yenileri bildirilir
        runCatching { repo.getRecentPings(20) }
            .onSuccess { pings ->
                pings.forEach { ping ->
                    val ts = runCatching { java.time.Instant.parse(ping.createdAt) }.getOrNull()
                    if (ts != null && ts.isAfter(startedAt)) return@forEach
                    seenPingIds.add(ping.id ?: return@forEach)
                }
            }
        checkAnniversaries()
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
                }
            }
        }
    }

    private suspend fun onNewPings() {
        runCatching { repo.getRecentPings(10) }.onSuccess { pings ->
            android.util.Log.d("MinimuvWatcher", "pings fetched: ${pings.size}")
            pings.forEach { ping ->
                val id = ping.id ?: return@forEach
                android.util.Log.d("MinimuvWatcher", "ping id=$id from=${ping.fromProfile} me=$profileId seen=${id in seenPingIds}")
                if (id in seenPingIds) return@forEach
                seenPingIds.add(id)
                if (ping.fromProfile == profileId) return@forEach
                val senderName = runCatching { profileRepo.getProfile(ping.fromProfile) }.getOrNull()
                NotificationHelper.show(
                    context,
                    "💌 ${senderName?.name ?: "Partnerin"} sana yazdı",
                    ping.message,
                    id = id.hashCode(),
                )
            }
        }
    }

    private suspend fun onTitlesChanged() {
        try {
            val current = repo.getTitles().associateBy { it.id }
            val previous = lastTitles
            current.values.forEach { t ->
                val old = previous[t.id]
                if (old != null) {
                    if (old.status != WatchStatus.COMPLETED.db && t.status == WatchStatus.COMPLETED.db) {
                        notify("🎉 ${t.title}", "Birlikte bitirdiniz! Tebrikler!")
                    }
                    if (old.status == WatchStatus.COMPLETED.db && t.status == WatchStatus.REWATCHING.db) {
                        notify("🔁 ${t.title}", "Yeniden izlemeye başladınız!")
                    }
                } else {
                    if (t.status == WatchStatus.PLAN.db) {
                        notify("✨ ${t.title}", "Sırada listesine yeni bir şey eklendi!")
                    } else if (t.status == WatchStatus.WATCHING.db) {
                        notify("▶️ ${t.title}", "Yeni bir serüven başladı!")
                    }
                }
            }
            lastTitles = current
        } catch (_: Exception) {
            // çevrimdışı — sessizce geç
        }
    }

    private suspend fun onProgressChanged() {
        try {
            // Ayrı moddaki başlıklar için bölüm kilometre taşları
            val titles = repo.getTitles()
            titles.forEach { title ->
                if (title.watchMode != "ayri") return@forEach
                val progress = repo.getEpisodeProgress(title.id)
                val mine = progress.firstOrNull { it.profileId == profileId }?.currentEpisode ?: 0
                val partner = progress.firstOrNull { it.profileId != profileId }?.currentEpisode ?: 0
                val milestone = (minOf(mine, partner) / 10) * 10
                if (milestone >= 10 && milestoneNotified[title.id] != milestone) {
                    milestoneNotified[title.id] = milestone
                    notify(
                        "🎊 ${title.title}",
                        "İkiniz de $milestone. bölümü geçtiniz!",
                    )
                }
            }
        } catch (_: Exception) {
            // çevrimdışı — sessizce geç
        }
    }

    suspend fun checkAnniversaries() {
        val titles = repo.getTitles()
        val today = LocalDate.now()
        titles.forEach { t ->
            val date = t.startDate ?: return@forEach
            runCatching {
                val d = LocalDate.parse(date)
                if (d.monthValue == today.monthValue && d.dayOfMonth == today.dayOfMonth && d.year < today.year) {
                    val years = Period.between(d, today).years
                    if (years >= 1) {
                        NotificationHelper.show(
                            context,
                            "💌 Tam $years yıl önce bugün",
                            "${t.title} dizisine başlamıştık… Ne günlerdi!",
                            id = t.id.hashCode(),
                            channel = NotificationHelper.CHANNEL_ANNIVERSARY,
                        )
                    }
                }
            }
        }
    }

    private fun notify(title: String, text: String) {
        NotificationHelper.show(context, title, text, (title.hashCode() + System.currentTimeMillis()).toInt())
    }
}
