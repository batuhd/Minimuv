package com.sinop.minimuv.core

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sinop.minimuv.data.SettingsStore
import com.sinop.minimuv.data.TitleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * FCM data-only mesajlarını işler. Mesajlar bildirimi doğrudan içermez —
 * bu servis Supabase'deki gerçek durumu kontrol edip (çift bildirim korumalı)
 * bildirimi kendisi gösterir.
 *
 * Uygulama açıkken realtime watcher olayı zaten bildirir; FCM yolu da
 * işlenir çünkü realtime bağlantısı sessizce kopmuş olabilir (BUG-008).
 * Çifte bildirim, tracker'ların kalıcı dedup durumu (snapshot/watermark)
 * sayesinde engellenir: aynı olay ikinci kez diff üretmez.
 */
class MinimuvMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: return
        scope.launch {
            runCatching { handle(type) }
                .onFailure { android.util.Log.e("MinimuvFcm", "mesaj işlenemedi ($type)", it) }
        }
    }

    private suspend fun handle(type: String) {
        val context = applicationContext
        val settings = SettingsStore(context)
        val prefs = settings.rawPrefs.first()
        val url = settings.urlFrom(prefs) ?: return
        val key = settings.keyFrom(prefs) ?: return
        val profileId = settings.profileFrom(prefs) ?: return
        SupabaseProvider.configure(url, key)
        NotificationHelper.ensureChannels(context)

        when (type) {
            "ping" -> PingNotifier.process(context, profileId)
            "title_new", "title_status" -> {
                val titles = runCatching { TitleRepository().getTitles() }.getOrNull() ?: return
                TitleTransitionTracker.diff(context, profileId, titles).forEach { (title, text) ->
                    NotificationHelper.show(
                        context, title, text,
                        (title.hashCode() + System.currentTimeMillis()).toInt(),
                    )
                }
            }
            "episode_progress" -> MilestoneTracker.process(context, profileId)
            "score" -> ScoreNotifier.process(context, profileId)
            "note" -> NoteNotifier.process(context, profileId, NoteKind.TITLE)
            "episode_note" -> NoteNotifier.process(context, profileId, NoteKind.EPISODE)
        }
    }

    override fun onNewToken(token: String) {
        scope.launch {
            runCatching { com.sinop.minimuv.data.TokenRepository(applicationContext).saveToken(token) }
                .onFailure { android.util.Log.e("MinimuvFcm", "token kaydedilemedi", it) }
        }
    }
}
