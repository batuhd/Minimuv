package com.sinop.minimuv.core

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.sinop.minimuv.R

/** Çark sesleri — SoundPool düşük gecikmeli, hızlı UI sesleri için idealdir. */
object WheelSound {

    @Volatile
    private var pool: SoundPool? = null
    private var tickId = 0
    private var winId = 0
    @Volatile
    private var ready = false
    @Volatile
    private var loadedCount = 0

    fun init(context: Context) {
        if (pool != null) return
        val sp = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        tickId = sp.load(context, R.raw.wheel_tick, 1)
        winId = sp.load(context, R.raw.wheel_win, 1)
        // Her iki ses de başarıyla yüklenmeden çalmaya başlama (BUG-041).
        sp.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                loadedCount += 1
                if (loadedCount >= 2) ready = true
            } else {
                android.util.Log.w("WheelSound", "ses yüklenemedi (status=$status)")
            }
        }
        pool = sp
    }

    fun playTick() {
        if (!ready) return
        pool?.play(tickId, 0.8f, 0.8f, 1, 0, 1f)
    }

    fun playWin() {
        if (!ready) return
        pool?.play(winId, 1f, 1f, 1, 0, 1f)
    }

    /** Ekran kapanınca SoundPool kaynaklarını serbest bırak (DEAD-021). */
    fun release() {
        pool?.release()
        pool = null
        ready = false
        loadedCount = 0
        tickId = 0
        winId = 0
    }
}