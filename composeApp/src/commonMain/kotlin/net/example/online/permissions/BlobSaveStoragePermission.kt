package net.example.online.permissions

import androidx.compose.runtime.Composable

/**
 * Привязка запроса доступа к хранилищу для [net.example.online.webview.SaveBlobJsHandler].
 * Android: moko-permissions WRITE_STORAGE (API < 29).
 * iOS / desktop: no-op (сохранение в sandbox без runtime permission).
 */
@Composable
expect fun BindBlobSaveStoragePermission()

internal object BlobSaveStoragePermission {
    private var requestAccess: ((onGranted: () -> Unit) -> Unit)? = null

    fun bind(request: (onGranted: () -> Unit) -> Unit) {
        requestAccess = request
    }

    fun unbind() {
        requestAccess = null
    }

    fun request(onGranted: () -> Unit) {
        val handler = requestAccess
        if (handler == null) {
            onGranted()
            return
        }
        handler(onGranted)
    }
}
