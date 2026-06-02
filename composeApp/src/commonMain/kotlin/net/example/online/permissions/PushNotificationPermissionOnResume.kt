package net.example.online.permissions

import androidx.compose.runtime.Composable

/**
 * Android / iOS: [moko-permissions](https://klibs.io/project/icerockdev/moko-permissions) на onResume.
 * JVM (desktop): no-op — у moko нет jvm-артефакта.
 */
@Composable
expect fun PushNotificationPermissionOnResume()
