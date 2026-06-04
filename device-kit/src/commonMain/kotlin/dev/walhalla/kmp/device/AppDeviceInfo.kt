package dev.walhalla.kmp.device

/**
 * Данные устройства/приложения для `Android.*` JSBridge
 */
data class AppDeviceInfo(
    val appName: String,
    val appVersionName: String,
    val appVersionCode: Int,
    val installSource: String,
    val deviceModel: String,
    val deviceManufacturer: String,
    /** Версия ОС: Android release / iOS systemVersion */
    val osVersion: String,
    val deviceId: String,
    val locale: String,
    val timeZone: String,
    /** Поля для POST `device/stats` (ldsonline `StatsSendWorker.createFields`). */
    val sdk: String,
    val board: String,
    val brand: String,
    /** `device` в stats: на Android `MANUFACTURER MODEL`. */
    val statsDevice: String,
    val hardware: String,
    val mac: String,
    val wifiFrequencyMhz: Int?,
)

expect fun provideLdsWebAppDeviceInfo(): AppDeviceInfo

/** JSON как `Android.getDeviceInfo()` в старом приложении. */
fun AppDeviceInfo.toGetDeviceInfoJson(): String {
    val appInfo = """
        {
            "appName": "${escapeJson(appName)}",
            "versionName": "${escapeJson(appVersionName)}",
            "versionCode": $appVersionCode,
            "installSource": "${escapeJson(installSource)}"
        }
    """.trimIndent()

    val deviceInfo = """
        {
            "model": "${escapeJson(deviceModel)}",
            "device_id": "${escapeJson(deviceId)}",
            "manufacturer": "${escapeJson(deviceManufacturer)}",
            "androidVersion": "${escapeJson(osVersion)}",
            "locale": "${escapeJson(locale)}",
            "timeZone": "${escapeJson(timeZone)}",
            "nestedData": {
                "someString": "This is a nested string",
                "someNumber": 12345
            }
        }
    """.trimIndent()

    return """
        {
            "appInfo": $appInfo,
            "deviceInfo": $deviceInfo
        }
    """.trimIndent()
}

private fun escapeJson(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")
