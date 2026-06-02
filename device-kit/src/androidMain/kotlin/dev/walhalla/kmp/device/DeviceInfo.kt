package dev.walhalla.kmp.device

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak")
object DeviceInfo {

    @Volatile
    var context: Context? = null
        private set
    fun initialize(context: Context) {
        if (this.context == null) {
            this.context = context.applicationContext
        }
    }
    fun requireContext(): Context =
        context ?: error("DeviceInfo is not initialized")
}