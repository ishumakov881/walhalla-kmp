package net.example.online.push

import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.web.WebViewNavigator

/**
 * Вызов из страницы ЛК: `Android.handleOpenPushMessagesClick()` (как [WebAppInterface] в appWebView).
 */
class OpenPushMessagesJsHandler(
    private val onOpenInbox: () -> Unit,
) : IJsMessageHandler {

    override fun methodName(): String = "handleOpenPushMessagesClick"

    override fun handle(
        message: JsMessage,
        navigator: WebViewNavigator?,
        callback: (String) -> Unit,
    ) {
        onOpenInbox()
        callback("ok")
    }
}
