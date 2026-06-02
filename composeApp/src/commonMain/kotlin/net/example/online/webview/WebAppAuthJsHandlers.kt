package net.example.online.webview

import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.web.WebViewNavigator

/** `Android.getUserSession()` */
class GetUserSessionJsHandler : IJsMessageHandler {
    override fun methodName(): String = "getUserSession"

    override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
        callback(WebAppSession.getUserSessionJson())
    }
}

/** `Android.onAuthorized(payload)` */
class OnAuthorizedJsHandler : IJsMessageHandler {
    override fun methodName(): String = "onAuthorized"

    override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
        WebAppSession.onAuthorized(message.params)
        callback("{}")
    }
}

/** `Android.onLogout()` */
class OnLogoutJsHandler : IJsMessageHandler {
    override fun methodName(): String = "onLogout"

    override fun handle(message: JsMessage, navigator: WebViewNavigator?, callback: (String) -> Unit) {
        WebAppSession.onLogout()
        callback("{}")
    }
}
