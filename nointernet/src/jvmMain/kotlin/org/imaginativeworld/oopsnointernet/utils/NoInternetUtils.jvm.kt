package org.imaginativeworld.oopsnointernet.utils

import java.awt.Desktop
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URI
import java.net.URL

actual object NoInternetUtils {

    actual fun isConnectedToInternet(): Boolean =
        runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence().any { networkInterface ->
                networkInterface.isUp &&
                    !networkInterface.isLoopback &&
                    networkInterface.inetAddresses.asSequence().any()
            }
        }.getOrDefault(false)

    actual fun isAirplaneModeOn(): Boolean = false

    actual fun isVpnActive(): Boolean =
        runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence().any { networkInterface ->
                networkInterface.isUp &&
                    VPN_INTERFACE_PREFIXES.any { prefix ->
                        networkInterface.name.startsWith(prefix, ignoreCase = true)
                    }
            }
        }.getOrDefault(false)

    actual fun hasActiveInternetConnection(): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(INTERNET_CHECK_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Test")
                setRequestProperty("Connection", "close")
                connectTimeout = 1_500
                readTimeout = 1_500
                connect()
            }
            connection.responseCode == HttpURLConnection.HTTP_OK
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    actual fun turnOnMobileData() {
        openNetworkSettings()
    }

    actual fun turnOffAirplaneMode() {
        openNetworkSettings()
    }

    actual fun turnOnWifi() {
        openNetworkSettings()
    }

    private fun openNetworkSettings() {
        if (!Desktop.isDesktopSupported()) {
            return
        }
        val uri = networkSettingsUri() ?: return
        runCatching {
            Desktop.getDesktop().browse(uri)
        }
    }

    private fun networkSettingsUri(): URI? {
        val os = System.getProperty("os.name").orEmpty().lowercase()
        val url = when {
            os.contains("win") -> "ms-settings:network"
            os.contains("mac") -> "x-apple.systempreferences:com.apple.preference.network"
            os.contains("nux") -> "gnome-control-center wifi"
            else -> null
        } ?: return null
        return runCatching { URI(url) }.getOrNull()
    }

    private const val INTERNET_CHECK_URL = "https://www.google.com"

    private val VPN_INTERFACE_PREFIXES = listOf(
        "tun",
        "tap",
        "ppp",
        "utun",
        "wg",
        "nordlynx",
        "wireguard",
    )
}
