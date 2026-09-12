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
        sp.setOnLoadCompleteListener { _, _, _ -> ready = true }
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
}