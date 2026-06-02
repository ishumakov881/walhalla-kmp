package net.example.online.push

/**
 * Вызов из `iOSApp.swift` при `didReceiveRemoteNotification` и т.п.
 */
fun handleRemotePushUserInfo(userInfo: Map<String, Any?>, sentTimeSec: Long = 0L) {
    PushInbox.handleRemoteNotification(userInfo, sentTimeSec)
}

fun requestOpenPushInbox() {
    PushInbox.requestOpenInbox()
}
