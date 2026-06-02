package net.example.online

import android.app.Application
import com.mmk.kmpnotifier.notification.NotifierManager
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import multiplatform.network.cmptoast.AppContext
import net.example.online.push.PushNotificationChannels

class MainApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.apply { set(applicationContext) }
        multiplatform.network.cmptoast.AppContext.apply { set(applicationContext) }

        PushNotificationChannels.create(this)

        NotifierManager.initialize(
            configuration = NotificationPlatformConfiguration.Android(
                notificationIconResId = R.mipmap.ic_logo,
                notificationIconColorResId = R.color.colorAccent,
                showPushNotification = true,
                notificationChannelData = NotificationPlatformConfiguration.Android.NotificationChannelData(
                    id = getString(R.string.msg_default_channel_id),
                    name = getString(R.string.msg_default_channel_name),
                    description = getString(R.string.msg_default_channel_description),
                ),
            ),
        )
    }
}
