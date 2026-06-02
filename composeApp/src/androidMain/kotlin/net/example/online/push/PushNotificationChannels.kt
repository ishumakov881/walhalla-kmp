package net.example.online.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import net.example.online.R

/**
 * Каналы и группа как в [net.example.online.Notification] старого appWebView (accesspoint).
 * Вызывать до [com.mmk.kmpnotifier.notification.NotifierManager.initialize], чтобы id совпали с FCM meta-data.
 */
object PushNotificationChannels {
    const val GROUP_ID: String = PushNotificationConfig.GROUP_ID

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val app = context.applicationContext
        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannelGroup(
            NotificationChannelGroup(GROUP_ID, app.getString(R.string.notif_group_name)),
        )

        createMessageChannel(
            app,
            manager,
            R.string.msg_default_channel_id,
            R.string.msg_default_channel_name,
            R.string.msg_default_channel_description,
            silent = false,
        )
        createMessageChannel(
            app,
            manager,
            R.string.msg_silent_channel_id,
            R.string.msg_silent_channel_name,
            R.string.msg_silent_channel_description,
            silent = true,
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createMessageChannel(
        context: Context,
        manager: NotificationManager,
        channelIdRes: Int,
        nameRes: Int,
        descriptionRes: Int,
        silent: Boolean,
    ) {
        val channel = NotificationChannel(
            context.getString(channelIdRes),
            context.getString(nameRes),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(descriptionRes)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableLights(true)
            group = GROUP_ID
            if (silent) {
                enableVibration(false)
                setSound(null, null)
            } else {
                enableVibration(true)
            }
        }
        manager.createNotificationChannel(channel)
    }
}
