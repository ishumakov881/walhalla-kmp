package net.example.online.webview

import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.WebViewJsBridge
import com.multiplatform.webview.web.WebViewNavigator
import dev.walhalla.kmp.device.AppDeviceInfo
import dev.walhalla.kmp.device.toGetDeviceInfoJson
import multiplatform.network.cmptoast.showToast
import net.example.online.push.OpenPushMessagesJsHandler

fun createSimpleHandler(
    name: String,
    valueProvider: () -> String,
): IJsMessageHandler = object : IJsMessageHandler {
    override fun methodName(): String = name
    override fun handle(
        message: JsMessage,
        navigator: WebViewNavigator?,
        callback: (String) -> Unit,
    ) {
        callback(valueProvider())
    }
}

/**
 * Регистрация методов `window.Android.*` как в appWebView [WebAppInterface].
 */
fun registerLdsWebAppJsHandlers(
    jsBridge: WebViewJsBridge,
    deviceInfo: AppDeviceInfo,
    pushTokenProvider: () -> String?,
    onOpenInbox: () -> Unit,
) {
    val info = deviceInfo
    val deviceInfoJson = info.toGetDeviceInfoJson()

    jsBridge.register(createSimpleHandler("getDeviceModel") { info.deviceModel })
    jsBridge.register(createSimpleHandler("getDeviceManufacturer") { info.deviceManufacturer })
    jsBridge.register(createSimpleHandler("getAndroidVersion") { info.osVersion })
    jsBridge.register(createSimpleHandler("getOSVersion") { info.osVersion })
    jsBridge.register(createSimpleHandler("getAppVersionName") { info.appVersionName })
    jsBridge.register(createSimpleHandler("getAppVersion") { info.appVersionName })
    jsBridge.register(createSimpleHandler("getAppVersionCode") { info.appVersionCode.toString() })
    jsBridge.register(createSimpleHandler("getDeviceId") { info.deviceId })
    jsBridge.register(createSimpleHandler("getLocale") { info.locale })
    jsBridge.register(createSimpleHandler("getTimeZone") { info.timeZone })
    jsBridge.register(createSimpleHandler("getInstallSource") { info.installSource })
    jsBridge.register(createSimpleHandler("getDeviceInfo") { deviceInfoJson })
    jsBridge.register(createSimpleHandler("getFbToken") { pushTokenProvider().orEmpty() })
    jsBridge.register(createSimpleHandler("getFcmToken") { pushTokenProvider().orEmpty() })
    jsBridge.register(createSimpleHandler("getFirebaseToken") { pushTokenProvider().orEmpty() })
    jsBridge.register(object : IJsMessageHandler {
        override fun methodName(): String = "showToast"
        override fun handle(
            message: JsMessage,
            navigator: WebViewNavigator?,
            callback: (String) -> Unit,
        ) {
            showToast(message = message.params)
            callback("{}")
        }
    })
    jsBridge.register(object : IJsMessageHandler {
        override fun methodName(): String = "Greet"
        override fun handle(
            message: JsMessage,
            navigator: WebViewNavigator?,
            callback: (String) -> Unit,
        ) {
            try {
                println("GreetHandler, Params: ${message.params}")
                showToast(message = message.params)
                //callback("{}")
            } catch (e: Exception) {
                println("GreetHandler, Error in handle $e")
                callback("{\"error\": \"${e.message}\"}")  // отправить ошибку в JS

            }
        }
    })
    jsBridge.register(GetUserSessionJsHandler())
    jsBridge.register(OnAuthorizedJsHandler())
    jsBridge.register(OnLogoutJsHandler())
    jsBridge.register(OpenPushMessagesJsHandler(onOpenInbox = onOpenInbox))
    jsBridge.register(SaveBlobJsHandler())
}
