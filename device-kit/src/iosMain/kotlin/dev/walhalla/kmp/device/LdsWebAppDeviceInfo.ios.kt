package dev.walhalla.kmp.device

import platform.Foundation.NSBundle
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.UIKit.UIDevice

actual fun provideLdsWebAppDeviceInfo(): AppDeviceInfo {
    MACKeeper.obtainMacAddress()
    val device = UIDevice.currentDevice
    val bundle = NSBundle.mainBundle
    val appName = bundle.objectForInfoDictionaryKey("CFBundleDisplayName") as? String
        ?: bundle.objectForInfoDictionaryKey("CFBundleName") as? String
        ?: "Example"
    val versionName = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0"
    val versionCode = (bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String)?.toIntOrNull() ?: 1
    @Suppress("UNCHECKED_CAST")
    val locale = bundle.preferredLocalizations.firstOrNull() as? String ?: "en"
    val timeZone = NSTimeZone.localTimeZone.name

    return AppDeviceInfo(
        appName = appName,
        appVersionName = versionName,
        appVersionCode = versionCode,
        installSource = "app_store",
        deviceModel = device.model,
        deviceManufacturer = "Apple",
        osVersion = device.systemVersion,
        deviceId = Installations.deviceId(),
        locale = locale,
        timeZone = timeZone,
        sdk = device.systemVersion,
        board = "",
        brand = "Apple",
        statsDevice = device.model,
        hardware = "",
        mac = MACKeeper.macAddress,
        wifiFrequencyMhz = connectedWifiFrequencyMhz(),
    )
}
