package net.lds.sip.android

import android.app.Notification
import android.app.Service
import android.os.Build
import androidx.core.app.NotificationCompat

internal object SipForegroundNotifications {
    fun promote(service: Service, spec: SipForegroundNotification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val context = service.applicationContext
        val builder = NotificationCompat.Builder(context, spec.channelId)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentTitle(spec.title(context))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSmallIcon(spec.smallIconResId)
        service.startForeground(spec.notificationId, builder.build())
    }
}
