package net.example.online.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
actual fun BindBlobSaveStoragePermission() {
    DisposableEffect(Unit) {
        BlobSaveStoragePermission.bind { onGranted -> onGranted() }
        onDispose { BlobSaveStoragePermission.unbind() }
    }
}
