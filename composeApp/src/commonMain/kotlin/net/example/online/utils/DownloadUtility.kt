package net.example.online.utils

import net.example.online.webview.WebViewAppConfig
import kotlin.time.Clock

expect object DownloadUtility {
    fun downloadFile(url: String, fileName: String)
}

fun isLinkInternal(url: String): Boolean {
    for (rule in WebViewAppConfig.LINKS_OPENED_IN_INTERNAL_WEBVIEW) {
        if (url.contains(rule)) return true
    }
    return false
}

fun getFileName(url: String): String {
    var url = url
    var index = url.indexOf("?")
    if (index > -1) {
        url = url.substring(0, index)
    }
    url = url.lowercase()

    index = url.lastIndexOf("/")
    return if (index > -1) {
        url.substring(index + 1)
    } else {
        Clock.System.now().toString()
    }
}