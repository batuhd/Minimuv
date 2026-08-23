package com.sinop.minimuv.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Telefon açılışında servisi başlatmayı dener (kısıtlanırsa uygulama açılınca başlar). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            runCatching { RealtimeService.start(context) }
        }
    }
}
