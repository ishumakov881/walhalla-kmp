package net.lds.online.sip

import android.media.AudioManager
import android.media.ToneGenerator

class TonePlayer(toneType: Int) : Thread() {
    private val mToneType: Int
    private val mDurationMs: Int
    private val mPauseMs: Int
    private var mPlaying = true

    init {
        if (TONE_DIAL_RINGBACK == toneType) {
            mToneType = ToneGenerator.TONE_SUP_DIAL
            mDurationMs = 1000
            mPauseMs = 5000
        } else {
            mToneType = ToneGenerator.TONE_PROP_BEEP
            mDurationMs = 1000
            mPauseMs = 1000
        }
    }

    override fun run() {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)

        while (mPlaying) {
            toneGenerator.startTone(mToneType, mDurationMs)
            synchronized(this) {
                try {
                    (this as Object).wait(mPauseMs.toLong())
                } catch (e: InterruptedException) {
                    // empty
                }
            }
        }

        toneGenerator.stopTone()
        toneGenerator.release()
    }

    fun stopTone() {
        synchronized(this) {
            mPlaying = false
            (this as Object).notify()
        }
    }

    companion object {
        const val TONE_DIAL_RINGBACK: Int = 0
        const val TONE_LOW_RSSI: Int = 1
    }
}
