package com.mmk.kmpnotifier.firebase

import com.google.firebase.messaging.RemoteMessage
import com.mmk.kmpnotifier.notification.NotifierManager
import net.example.online.push.PushInbox
import net.example.online.push.PushKeys


class ExampleFirebaseMessagingService : MyFirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payloadData = message.data
        if (!payloadData.containsKey("commands")) {
            var title = payloadData[PushKeys.TITLE]
            var body = payloadData[PushKeys.BODY]
            if (title.isNullOrEmpty()) title = message.notification?.title
            if (body.isNullOrEmpty()) body = message.notification?.body

            PushInbox.handleFcmMessage(
                data = payloadData.mapValues { it.value as Any? },
                notificationTitle = message.notification?.title,
                notificationBody = message.notification?.body,
                sentTimeSec = message.sentTime.takeIf { it > 0L }?.div(1000) ?: 0L,
            )

            // data-only: система сама баннер не покажет — как sendNotification() в accesspoint
            if (message.notification == null && (!title.isNullOrEmpty() || !body.isNullOrEmpty())) {
                NotifierManager.getLocalNotifier().notify(
                    title = title.orEmpty(),
                    body = body.orEmpty(),
                    payloadData = payloadData,
                )
            }
        }
        super.onMessageReceived(message)
    }
}
