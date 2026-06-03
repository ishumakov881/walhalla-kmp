package dev.walhalla.kmp.device

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

private const val DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00"
private val INTERFACE_NAME_PREFERENCE = listOf("wlan", "wifi", "en", "eth")

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

    @Synchronized
    actual fun obtainMacAddress() {
        if (sMacAddress.isNotEmpty() && sMacAddress != DEFAULT_MAC_ADDRESS && sIpAddress.isNotEmpty()) {
            return
        }

        val networkInterface = findPreferredNetworkInterface()
        if (networkInterface == null) {
            resetToDefaults()
            return
        }

        sIpAddress = networkInterface.inet4HostAddress().orEmpty()
        sMacAddress = networkInterface.hardwareMacAddress() ?: DEFAULT_MAC_ADDRESS
        sDefaultGateway = readDefaultGateway().orEmpty()
        // JVM/desktop has no WiвЂ‘Fi band API equivalent to Android WifiManager.
        sIs5GHzBandSupported = false

        if (sMacAddress.isEmpty()) {
            sMacAddress = DEFAULT_MAC_ADDRESS
        }
    }

    private fun resetToDefaults() {
        sIpAddress = ""
        sDefaultGateway = ""
        sIs5GHzBandSupported = false
        sMacAddress = DEFAULT_MAC_ADDRESS
    }
}

private fun findPreferredNetworkInterface(): NetworkInterface? {
    val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
    return interfaces
        .filter { it.isUp && !it.isLoopback && it.inet4HostAddress() != null }
        .minByOrNull { interfacePreferenceRank(it.name) }
}

private fun interfacePreferenceRank(name: String): Int {
    val lower = name.lowercase(Locale.US)
    val index = INTERFACE_NAME_PREFERENCE.indexOfFirst { lower.contains(it) }
    return if (index < 0) INTERFACE_NAME_PREFERENCE.size else index
}

private fun NetworkInterface.inet4HostAddress(): String? {
    return interfaceAddresses
        ?.asSequence()
        ?.mapNotNull { it.address }
        ?.filterIsInstance<Inet4Address>()
        ?.firstOrNull { !it.isLoopbackAddress }
        ?.hostAddress
}

private fun NetworkInterface.hardwareMacAddress(): String? {
    val mac = hardwareAddress ?: return null
    if (mac.isEmpty()) return null
    return mac.joinToString(":") { byte ->
        String.format(Locale.US, "%02X", byte)
    }
}

private fun readDefaultGateway(): String? {
    val os = System.getProperty("os.name")?.lowercase(Locale.US).orEmpty()
    return when {
        os.contains("win") -> readWindowsGateway()
        os.contains("mac") || os.contains("darwin") -> readMacOsGateway()
        else -> readLinuxGateway()
    }
}

private fun readLinuxGateway(): String? {
    return try {
        val lines = java.io.File("/proc/net/route").readLines()
        for (line in lines.drop(1)) {
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 3 && parts[1] == "00000000") {
                return hexLittleEndianToIpv4(parts[2])
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

private fun readMacOsGateway(): String? {
    return try {
        val process = ProcessBuilder("route", "-n", "get", "default")
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().useLines { lines ->
            lines.firstOrNull { it.trim().startsWith("gateway:", ignoreCase = true) }
                ?.substringAfter(':')
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }
    } catch (_: Exception) {
        null
    }
}

private fun readWindowsGateway(): String? {
    return try {
        val process = ProcessBuilder("cmd", "/c", "route", "print", "0.0.0.0")
            .redirectErrorStream(true)
            .start()
        val lines = process.inputStream.bufferedReader().readLines()
        lines.asSequence()
            .dropWhile { !it.trim().startsWith("0.0.0.0") }
            .drop(1)
            .firstOrNull()
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.getOrNull(2)
            ?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }
}

private fun hexLittleEndianToIpv4(hex: String): String? {
    if (hex.length != 8) return null
    return try {
        (0 until 4)
            .map { index ->
                hex.substring(index * 2, index * 2 + 2).toInt(16)
            }
            .joinToString(".") { octet -> octet.toString() }
    } catch (_: Exception) {
        null
    }
}

actual fun connectedWifiFrequencyMhz(): Int? = null
