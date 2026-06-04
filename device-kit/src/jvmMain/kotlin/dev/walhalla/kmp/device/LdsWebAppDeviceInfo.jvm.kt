package dev.walhalla.kmp.device

import java.util.Locale
import java.util.TimeZone

actual fun provideLdsWebAppDeviceInfo(): AppDeviceInfo {
    MACKeeper.obtainMacAddress()
    val osName = System.getProperty("os.name").orEmpty()
    val osVersion = System.getProperty("os.version").orEmpty()
    return AppDeviceInfo(
        appName = "Example Desktop",
        appVersionName = "1.0",
        appVersionCode = 1,
        installSource = "direct",
        deviceModel = osName,
        deviceManufacturer = "Desktop",
        osVersion = osVersion,
        deviceId = Installations.deviceId(),
        locale = Locale.getDefault().toString(),
        timeZone = TimeZone.getDefault().id,
        sdk = "",
        board = "",
        brand = "Desktop",
        statsDevice = osName,
        hardware = "",
        mac = MACKeeper.macAddress,
        wifiFrequencyMhz = connectedWifiFrequencyMhz(),
    )
}
