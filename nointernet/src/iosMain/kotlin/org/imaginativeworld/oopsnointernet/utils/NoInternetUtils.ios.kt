package org.imaginativeworld.oopsnointernet.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CFNetwork.CFNetworkCopySystemProxySettings
import platform.CoreFoundation.CFCopyDescription
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.Foundation.NSURL
import platform.SystemConfiguration.SCNetworkReachabilityCreateWithName
import platform.SystemConfiguration.SCNetworkReachabilityFlagsVar
import platform.SystemConfiguration.SCNetworkReachabilityGetFlags
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsConnectionRequired
import platform.SystemConfiguration.kSCNetworkReachabilityFlagsReachable
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@OptIn(ExperimentalForeignApi::class)
actual object NoInternetUtils {

    actual fun isConnectedToInternet(): Boolean = isReachableViaSystemConfiguration(REACHABILITY_HOST)

    actual fun isAirplaneModeOn(): Boolean = false

    actual fun isVpnActive(): Boolean {
        val settings: CFDictionaryRef? = CFNetworkCopySystemProxySettings() ?: return false
        val description = copyCfDescription(settings)
        CFRelease(settings)
        if (!description.contains("__SCOPED__")) {
            return false
        }
        return VPN_INTERFACE_PREFIXES.any { prefix ->
            description.contains(prefix, ignoreCase = true)
        }
    }

    actual fun hasActiveInternetConnection(): Boolean = isConnectedToInternet()

    actual fun turnOnMobileData() {
        openSystemUrl(MOBILE_DATA_SETTINGS_URL)
    }

    actual fun turnOffAirplaneMode() {
        openSystemUrl(AIRPLANE_MODE_SETTINGS_URL)
    }

    actual fun turnOnWifi() {
        openSystemUrl(WIFI_SETTINGS_URL)
    }

    private fun copyCfDescription(value: CFTypeRef?): String {
        if (value == null) {
            return ""
        }
        val descriptionRef = CFCopyDescription(value) ?: return ""
        val text = descriptionRef.toString()
        CFRelease(descriptionRef)
        return text
    }

    private fun isReachableViaSystemConfiguration(host: String): Boolean = memScoped {
        val reachability = SCNetworkReachabilityCreateWithName(null, host) ?: return false
        val flags = alloc<SCNetworkReachabilityFlagsVar>()
        if (!SCNetworkReachabilityGetFlags(reachability, flags.ptr)) {
            return false
        }
        val value = flags.value
        val reachable = (value and kSCNetworkReachabilityFlagsReachable) != 0u
        val needsConnection = (value and kSCNetworkReachabilityFlagsConnectionRequired) != 0u
        reachable && !needsConnection
    }

    private fun openSystemUrl(urlString: String) {
        val url = NSURL.URLWithString(urlString)
        val application = UIApplication.sharedApplication
        if (url != null && application.canOpenURL(url)) {
            application.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        } else {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(
            url,
            options = emptyMap<Any?, Any>(),
            completionHandler = null,
        )
    }

    private const val REACHABILITY_HOST = "www.google.com"
    private const val WIFI_SETTINGS_URL = "App-Prefs:root=WIFI"
    private const val MOBILE_DATA_SETTINGS_URL = "App-Prefs:root=MOBILE_DATA_SETTINGS_ID"
    private const val AIRPLANE_MODE_SETTINGS_URL = "App-Prefs:root=AIRPLANE_MODE"

    private val VPN_INTERFACE_PREFIXES = listOf("utun", "ppp", "ipsec", "tap", "tun")
}
