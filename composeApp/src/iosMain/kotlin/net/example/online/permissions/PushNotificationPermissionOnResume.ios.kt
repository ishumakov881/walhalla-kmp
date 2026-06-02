package net.example.online.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION
import kotlinx.coroutines.launch

@Composable
actual fun PushNotificationPermissionOnResume() {
    val factory = rememberPermissionsControllerFactory()
    val permissionsController = remember(factory) { factory.createPermissionsController() }
    BindEffect(permissionsController)
    val scope = rememberCoroutineScope()

    LifecycleResumeEffect(permissionsController) {
        val job = scope.launch {
            if (permissionsController.isPermissionGranted(Permission.REMOTE_NOTIFICATION)) {
                return@launch
            }
            try {
                permissionsController.providePermission(Permission.REMOTE_NOTIFICATION)
            } catch (_: DeniedException) {
            } catch (_: DeniedAlwaysException) {
            }
        }
        onPauseOrDispose { job.cancel() }
    }
}
