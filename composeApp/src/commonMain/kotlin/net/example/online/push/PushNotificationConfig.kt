package net.example.online.push

/**
 * Id каналов и группы — как в старом appWebView (accesspoint).
 * На Android совпадают с [R.string.msg_default_channel_id] и FCM meta-data.
 */
object PushNotificationConfig {
    const val GROUP_ID: String = "LDS_online_group_01"
    const val DEFAULT_CHANNEL_ID: String = "channel.messages.default"
    const val SILENT_CHANNEL_ID: String = "channel.messages.silent"
}
