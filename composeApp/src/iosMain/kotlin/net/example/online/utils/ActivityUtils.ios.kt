package net.example.online.utils

import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

actual object ActivityUtils {

    actual fun openBrowser(data: String) {
        openUrl(data)
    }

    actual fun startEmailActivity(email: String, subject: String?, text: String?) {
        val query = buildList {
            subject?.let { add("subject=${it.encodeUrlQueryValue()}") }
            text?.let { add("body=${it.encodeUrlQueryValue()}") }
        }.joinToString("&")
        val url = if (query.isEmpty()) "mailto:$email" else "mailto:$email?$query"
        openUrl(url)
    }

    actual fun startCallActivity(url: String) {
        openUrl(url)
    }

    actual fun startSmsActivity(url: String) {
        openUrl(url)
    }

    actual fun startMapSearchActivity(url: String) {
        starDefault(url)
    }

    actual fun startMapYandex(url: String) {
        openUrl(url)
    }

    actual fun startyandexnavi(url: String) {
        starDefault(url)
    }

    actual fun starDefault(url: String) {
        openUrl(url)
    }

    actual fun startShareActivity(subject: String?, text: String) {
        val items = buildList {
            add(text)
            subject?.let { add(it) }
        }
        val controller = UIActivityViewController(items, null)
        topViewController()?.presentViewController(controller, animated = true, completion = null)
    }

    actual fun starttg(url: String) {
        openUrl(url)
    }

    actual fun startViber(url: String) {
        if (!openUrl(url)) {
            openUrl("https://apps.apple.com/app/viber-messenger-chats-calls/id382617920")
        }
    }

    actual fun sendWhatsappText(url: String) {
        if (!openUrl(url)) {
            val text = extractQueryParam(url, "text")
            val fallback = if (text != null) {
                "https://wa.me/?text=${text.encodeUrlQueryValue()}"
            } else {
                "https://apps.apple.com/app/whatsapp-messenger/id310633997"
            }
            openUrl(fallback)
        }
    }

    actual fun sendWhatsappPhone(url: String) {
        if (!openUrl(url)) {
            openUrl("https://apps.apple.com/app/whatsapp-messenger/id310633997")
        }
    }

    actual fun mailTo(url: String) {
        openUrl(url)
    }

    private fun openUrl(url: String): Boolean {
        val nsUrl = NSURL.URLWithString(url) ?: return false
        val application = UIApplication.sharedApplication
        if (!application.canOpenURL(nsUrl)) {
            return false
        }
        application.openURL(
            nsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
        return true
    }

    private fun topViewController(): UIViewController? {
        val window = UIApplication.sharedApplication.keyWindow
            ?: UIApplication.sharedApplication.windows.firstOrNull() as? platform.UIKit.UIWindow
        var controller = window?.rootViewController
        while (controller?.presentedViewController != null) {
            controller = controller.presentedViewController
        }
        return controller
    }

    private fun extractQueryParam(url: String, param: String): String? {
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return null
        return url.substring(queryStart + 1)
            .split('&')
            .firstNotNullOfOrNull { part ->
                val eq = part.indexOf('=')
                if (eq > 0 && part.substring(0, eq) == param) {
                    part.substring(eq + 1)
                } else {
                    null
                }
            }
    }

    private fun String.encodeUrlQueryValue(): String {
        val encoded = (this as NSString).stringByAddingPercentEncodingWithAllowedCharacters(
            NSCharacterSet.URLQueryAllowedCharacterSet,
        )
        return encoded ?: this
    }
}
