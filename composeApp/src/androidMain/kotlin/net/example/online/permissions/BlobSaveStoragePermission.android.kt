package net.example.online.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.storage.WRITE_STORAGE
import kotlinx.coroutines.launch
import multiplatform.network.cmptoast.showToast

@Composable
actual fun BindBlobSaveStoragePermission() {
    val factory = rememberPermissionsControllerFactory()
    val permissionsController = remember(factory) { factory.createPermissionsController() }
    BindEffect(permissionsController)
    val scope = rememberCoroutineScope()

    DisposableEffect(permissionsController, scope) {
        BlobSaveStoragePermission.bind { onGranted ->
            scope.launch {
                try {
                    if (!permissionsController.isPermissionGranted(Permission.WRITE_STORAGE)) {
                        permissionsController.providePermission(Permission.WRITE_STORAGE)
                    }
                    onGranted()
                } catch (_: DeniedException) {
                    showToast("Нет доступа к памяти. Проверьте разрешения.")
                } catch (_: DeniedAlwaysException) {
                    showToast("Разрешите доступ к памяти в настройках приложения.")
                }
            }
        }
        onDispose { BlobSaveStoragePermission.unbind() }
    }
}
