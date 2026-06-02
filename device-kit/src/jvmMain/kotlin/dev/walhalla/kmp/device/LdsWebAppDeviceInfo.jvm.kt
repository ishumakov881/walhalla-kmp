package dev.walhalla.kmp.device

import java.util.Locale
import java.util.TimeZone

actual fun provideLdsWebAppDeviceInfo(): AppDeviceInfo = AppDeviceInfo(
    appName = "Example Desktop",
    appVersionName = "1.0",
    appVersionCode = 1,
    installSource = "direct",
    deviceModel = System.getProperty("os.name").orEmpty(),
    deviceManufacturer = "Desktop",
    osVersion = System.getProperty("os.version").orEmpty(),
    deviceId = Installations.deviceId(),
    locale = Locale.getDefault().toString(),
    timeZone = TimeZone.getDefault().id,
)
