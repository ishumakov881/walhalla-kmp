package dev.walhalla.kmp.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

private const val DEFAULT_MAC_ADDRESS_STR = DEFAULT_MAC_ADDRESS

actual object MACKeeper {
    private var sIpAddress: String = ""
    private var sDefaultGateway: String = ""
    private var sIs5GHzBandSupported = false
    private var sMacAddress: String = ""

    actual val ipAddress: String
        get() = sIpAddress

    actual val defaultGateway: String
        get() = sDefaultGateway

    actual val macAddress: String
        get() = sMacAddress

    actual fun is5GHzBandSupported(): Boolean = sIs5GHzBandSupported

    actual fun obtainMacAddress() {
        val context = DeviceInfo.context
        if (context == null) {
            sMacAddress = DEFAULT_MAC_ADDRESS_STR
            return
        }
        obtainMacAddress(context)
    }

    @JvmStatic
    @Synchronized
    fun obtainMacAddress(context: Context) {
        if (sMacAddress.isNotEmpty() && sMacAddress != DEFAULT_MAC_ADDRESS_STR && sIpAddress.isNotEmpty()) {
            return
        }

        val appCtx = context.applicationContext
        val wfm = appCtx.getSystemService(Context.WIFI_SERVICE) as WifiManager?

        if (wfm == null) {
            sMacAddress = DEFAULT_MAC_ADDRESS_STR
            return
        }

        if (appCtx.checkCallingOrSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            sMacAddress = DEFAULT_MAC_ADDRESS_STR
            return
        }

        val wifiInfo: WifiInfo? = wfm.connectionInfo

        if (wifiInfo != null) {
            sIpAddress = formatIpAddress(wifiInfo.ipAddress)

            val dhcpInfo = wfm.dhcpInfo
            sDefaultGateway = if (dhcpInfo != null) formatIpAddress(dhcpInfo.gateway) else ""

            sIs5GHzBandSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && wfm.is5GHzBandSupported

            var currentMac = wifiInfo.macAddress
            if (currentMac == null || currentMac == DEFAULT_MAC_ADDRESS_STR) {
                currentMac = getMacAddressViaNetworkInterface() ?: DEFAULT_MAC_ADDRESS_STR
            }
            sMacAddress = currentMac
        } else {
            sIpAddress = ""
            sDefaultGateway = ""
            sIs5GHzBandSupported = false
            sMacAddress = DEFAULT_MAC_ADDRESS_STR
        }

        if (sMacAddress.isEmpty()) {
            sMacAddress = DEFAULT_MAC_ADDRESS_STR
        }
    }

    private fun getMacAddressViaNetworkInterface(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (iface in interfaces) {
                if (!iface.name.contains("wlan", ignoreCase = true)) continue

                val mac = iface.hardwareAddress ?: continue

                val buf = StringBuilder()
                for (b in mac) {
                    buf.append(String.format("%02X:", b))
                }
                if (buf.isNotEmpty()) {
                    buf.deleteCharAt(buf.length - 1)
                }
                val macAddressString = buf.toString()
                return macAddressString
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun formatIpAddress(ipAddress: Int): String =
        String.format(
            Locale.US,
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            (ipAddress shr 8) and 0xff,
            (ipAddress shr 16) and 0xff,
            (ipAddress shr 24) and 0xff,
        )
}

actual fun connectedWifiFrequencyMhz(): Int? {
    val context = DeviceInfo.context ?: return null
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null

    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return null
    @Suppress("DEPRECATION")
    val networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
    if (networkInfo == null || !networkInfo.isConnected) return null

    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        ?: return null
    @Suppress("DEPRECATION")
    return wifiManager.connectionInfo?.frequency
}
