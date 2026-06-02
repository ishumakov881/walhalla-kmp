package net.example.online.utils

actual object DownloadUtility {
    actual fun downloadFile(url: String, fileName: String) {
        JvmDesktopUtils.downloadFile(url, fileName)
    }
}
