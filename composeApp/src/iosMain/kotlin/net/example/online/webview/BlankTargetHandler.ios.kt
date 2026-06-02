package net.example.online.webview

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject

private val blankDelegates = mutableMapOf<Long, WKUIDelegateProtocol>()

actual fun configureBlankTargetHandling(nativeWebView: Any) {
    val wk = nativeWebView as? WKWebView ?: return
    val delegate = BlankTargetUiDelegate()
    wk.UIDelegate = delegate
    blankDelegates[wk.hash.toLong()] = delegate
}

private class BlankTargetUiDelegate : NSObject(), WKUIDelegateProtocol {
    override fun webView(
        webView: WKWebView,
        createWebViewWithConfiguration: WKWebViewConfiguration,
        forNavigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures,
    ): WKWebView? {
        val targetFrame = forNavigationAction.targetFrame
        if (targetFrame == null) {
            val url: NSURL? = forNavigationAction.request.URL
            if (url != null) {
                val app = UIApplication.sharedApplication
                if (app.canOpenURL(url)) {
                    app.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
                }
            }
        }
        return null
    }
}

