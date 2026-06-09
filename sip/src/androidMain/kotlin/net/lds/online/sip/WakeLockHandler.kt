package net.lds.online.sip

import android.app.Activity
import android.content.Context
import android.os.PowerManager
import android.os.PowerManager.WakeLock

class WakeLockHandler {
    private var mWakeLock: WakeLock? = null

    fun enableProximityScreenOff(activity: Activity) {
        if (null != mWakeLock) {
            mWakeLock!!.acquire()
            return
        }

        var field: Int
        try {
            // Yeah, this is a hidden field.
            field = PowerManager::class.java.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null)
        } catch (t: Throwable) {
            field = 32 // ???
        }

        val powerManager = activity.applicationContext
            .getSystemService(Context.POWER_SERVICE) as PowerManager?
        if (null != powerManager) {
            mWakeLock = powerManager.newWakeLock(field, activity.localClassName)
        }
    }

    fun disableProximityScreenOff(removeLock: Boolean) {
        if (null != mWakeLock && mWakeLock!!.isHeld) {
            mWakeLock!!.release()
        }
        if (removeLock) {
            mWakeLock = null
        }
    }
}
