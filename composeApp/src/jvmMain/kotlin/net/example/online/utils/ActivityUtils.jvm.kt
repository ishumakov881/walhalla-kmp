package net.example.online.utils

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

actual object ActivityUtils {

    actual fun openBrowser(data: String) {
        JvmDesktopUtils.openUrl(data)
    }

    actual fun startEmailActivity(email: String, subject: String?, text: String?) {
        JvmDesktopUtils.openMailto(email, subject, text)
    }

    actual fun startCallActivity(url: String) {
        JvmDesktopUtils.openUrl(url)
    }

    actual fun startSmsActivity(url: String) {
        JvmDesktopUtils.openUrl(url)
    }

    actual fun startMapSearchActivity(url: String) {
        starDefault(url)
    }

    actual fun startMapYandex(url: String) {
        JvmDesktopUtils.openUrl(url)
    }

    actual fun startyandexnavi(url: String) {
        starDefault(url)
    }

    actual fun starDefault(url: String) {
        JvmDesktopUtils.openUrl(url)
    }

    actual fun startShareActivity(subject: String?, text: String) {
        JvmDesktopUtils.shareText(subject, text)
    }

    actual fun starttg(url: String) {
        val normalized = when {
            url.startsWith("tg:") -> "https://t.me/${url.removePrefix("tg:").trimStart('/')}"
            else -> url
        }
        JvmDesktopUtils.openUrl(normalized)
    }

    actual fun startViber(url: String) {
        if (!JvmDesktopUtils.openUrl(url)) {
            JvmDesktopUtils.openUrl("https://www.viber.com/download/")
        }
    }

    actual fun sendWhatsappText(url: String) {
        if (!JvmDesktopUtils.openUrl(url)) {
            val text = JvmDesktopUtils.extractQueryParam(url, "text")
            val fallback = if (text != null) {
                "https://wa.me/?text=${encodeQuery(text)}"
            } else {
                "https://web.whatsapp.com/"
            }
            JvmDesktopUtils.openUrl(fallback)
        }
    }

    actual fun sendWhatsappPhone(url: String) {
        if (!JvmDesktopUtils.openUrl(url)) {
            JvmDesktopUtils.openUrl("https://web.whatsapp.com/")
        }
    }

    actual fun mailTo(url: String) {
        JvmDesktopUtils.mailTo(url)
    }

    private fun encodeQuery(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
