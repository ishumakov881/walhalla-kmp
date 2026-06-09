package net.lds.sip.android

import android.content.Context

data class SipForegroundNotification(
    val channelId: String,
    val notificationId: Int,
    val smallIconResId: Int,
    val title: (Context) -> CharSequence,
)

data class SipBroadcastConfig(
    val namespace: String,
)

data class SipConfiguration(
    val foreground: SipForegroundNotification,
    val broadcast: SipBroadcastConfig,
    val ensureChannels: (Context) -> Unit,
)

object SipRuntime {
    private lateinit var configuration: SipConfiguration

    fun init(configuration: SipConfiguration) {
        this.configuration = configuration
    }

    internal fun require(): SipConfiguration {
        check(::configuration.isInitialized) {
            "Call SipRuntime.init(SipConfiguration) from Application.onCreate"
        }
        return configuration
    }
}
