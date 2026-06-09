package net.lds.online.sip

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build

object ServiceUtils {
    fun startSipService(context: Context) {
        val intent: Intent? = Intent(context, SipService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
