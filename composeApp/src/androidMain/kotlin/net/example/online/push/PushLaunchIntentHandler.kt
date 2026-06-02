package net.example.online.push

import android.content.Intent

/**

 * система показала notification — письмо сохраняем при открытии по тапу из extras Intent.
 */
object PushLaunchIntentHandler {

    private const val ACTION_NOTIFICATION_CLICK =
        "com.mmk.kmpnotifier.notification.ACTION_NOTIFICATION_CLICK"

    /**
     * @param fromNewIntent `true` — activity жива (тап с главного); inbox откроет [NotifierManager.Listener.onNotificationClicked].
     * `false` — cold start / пересоздание с пуш-Intent; inbox открываем здесь (listener ещё не подписан).
     */
    fun handle(intent: Intent?, fromNewIntent: Boolean) {
        if (intent == null) return
        val extras = intent.extras ?: return

        val openedFromPush =
            extras.getString("google.message_id") != null ||
                extras.containsKey(ACTION_NOTIFICATION_CLICK) ||
                extras.getString(PushKeys.TITLE) != null

        if (!openedFromPush) return

        val data = linkedMapOf<String, Any?>()
        for (key in extras.keySet()) {
            if (key == ACTION_NOTIFICATION_CLICK) continue
            when (val value = extras.get(key)) {
                is String -> data[key] = value
                is Long -> data[key] = value
                is Int -> data[key] = value
                is Boolean -> data[key] = value
                else -> Unit
            }
        }

        val sentTimeRaw = extras.getLong("google.sent_time", 0L)
        val sentTimeSec = when {
            sentTimeRaw > 1_000_000_000_000L -> sentTimeRaw / 1000
            sentTimeRaw > 0L -> sentTimeRaw
            else -> 0L
        }


        //Проверка, должны быть поля, валидируем что Intent проброшен системой когда приложение выгружено
        if(!data.containsKey(PushKeys.TITLE) && !data.containsKey(PushKeys.BODY)) return

        PushInbox.handleFcmMessage(data = data, sentTimeSec = sentTimeSec)
        if (!fromNewIntent) {
            PushInbox.requestOpenInbox()
        }
    }
}
