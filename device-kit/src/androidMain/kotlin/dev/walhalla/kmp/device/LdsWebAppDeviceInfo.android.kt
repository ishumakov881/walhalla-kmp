package dev.walhalla.kmp.device

import android.content.pm.PackageManager
import android.os.Build

import java.util.Locale
import java.util.TimeZone

actual fun provideLdsWebAppDeviceInfo(): AppDeviceInfo {
    val context = DeviceInfo.context
        ?: return AppDeviceInfo(
            appName = "Example",
            appVersionName = "1.0",
            appVersionCode = 1,
            installSource = "direct",
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER,
            osVersion = Build.VERSION.RELEASE,
            deviceId = "",
            locale = Locale.getDefault().toString(),
            timeZone = TimeZone.getDefault().id,
        )

    val packageManager = context.packageManager
    val packageName = context.packageName
    val appLabel = context.applicationInfo.loadLabel(packageManager).toString()

    val versionName: String
    val versionCode: Int
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val info = packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        versionName = info.versionName ?: "1.0"
        versionCode = info.longVersionCode.toInt()
    } else {
        @Suppress("DEPRECATION")
        val info = packageManager.getPackageInfo(packageName, 0)
        versionName = info.versionName ?: "1.0"
        @Suppress("DEPRECATION")
        versionCode = info.versionCode
    }

    return AppDeviceInfo(
        appName = appLabel,
        appVersionName = versionName,
        appVersionCode = versionCode,
        installSource = resolveInstallSource(packageName, packageManager),
        deviceModel = Build.MODEL,
        deviceManufacturer = Build.MANUFACTURER,
        osVersion = Build.VERSION.RELEASE,
        deviceId = Installations.deviceId(),
        locale = Locale.getDefault().toString(),
        timeZone = TimeZone.getDefault().id,
    )
}

private fun resolveInstallSource(
    packageName: String,
    packageManager: PackageManager,
): String {
    val installer = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName)
        }
    } catch (_: Exception) {
        null
    }
    return installer ?: "direct"
}
