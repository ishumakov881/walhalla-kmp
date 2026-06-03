package dev.walhalla.kmp.device

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_ntop
import platform.posix.AF_INET
import platform.posix.AF_LINK
import platform.posix.INET_ADDRSTRLEN
import platform.posix.sockaddr
import platform.posix.sockaddr_in
import kotlin.experimental.and

private const val DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00"
private val PREFERRED_INTERFACE_NAMES = listOf("en0", "wlan0", "en1")

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

    @OptIn(ExperimentalForeignApi::class)
    actual fun obtainMacAddress() {
        if (sMacAddress.isNotEmpty() && sMacAddress != DEFAULT_MAC_ADDRESS && sIpAddress.isNotEmpty()) {
            return
        }

        val interfaceInfo = readInterfaceInfoFromGetIfAddrs()
        if (interfaceInfo == null) {
            resetToDefaults()
            return
        }

        sIpAddress = interfaceInfo.ipAddress
        sMacAddress = interfaceInfo.macAddress.ifEmpty { DEFAULT_MAC_ADDRESS }
        sDefaultGateway = ""
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

private data class InterfaceInfo(
    val ipAddress: String,
    val macAddress: String,
)

@OptIn(ExperimentalForeignApi::class)
private fun readInterfaceInfoFromGetIfAddrs(): InterfaceInfo? {
    memScoped {
        val ifaddrPtr = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(ifaddrPtr.ptr) != 0) return null

        val ips = mutableMapOf<String, String>()
        val macs = mutableMapOf<String, String>()

        try {
            var ptr: CPointer<ifaddrs>? = ifaddrPtr.value
            while (ptr != null) {
                val ifa = ptr.pointed
                val name = ifa.ifa_name?.toKString() ?: ""
                if (isPreferredInterfaceName(name)) {
                    ifa.ifa_addr?.let { addr ->
                        when (addr.pointed.sa_family.toInt()) {
                            AF_INET -> {
                                readIpv4Address(addr)?.takeIf { it.isNotEmpty() }?.let { ip ->
                                    ips[name] = ip
                                }
                            }
                            AF_LINK -> {
                                readMacAddress(addr)?.takeIf { it.isNotEmpty() }?.let { mac ->
                                    macs[name] = mac
                                }
                            }
                        }
                    }
                }
                ptr = ifa.ifa_next
            }
        } finally {
            freeifaddrs(ifaddrPtr.value)
        }

        for (preferred in PREFERRED_INTERFACE_NAMES) {
            val ip = ips[preferred] ?: continue
            return InterfaceInfo(
                ipAddress = ip,
                macAddress = macs[preferred].orEmpty(),
            )
        }

        val fallbackName = ips.keys.firstOrNull() ?: return null
        return InterfaceInfo(
            ipAddress = ips[fallbackName].orEmpty(),
            macAddress = macs[fallbackName].orEmpty(),
        )
    }
}

private fun isPreferredInterfaceName(name: String): Boolean {
    val lower = name.lowercase()
    return lower == "en0" ||
        lower == "en1" ||
        lower.startsWith("wlan") ||
        lower.startsWith("pdp_ip")
}

@OptIn(ExperimentalForeignApi::class)
private fun readIpv4Address(addr: CPointer<sockaddr>): String? {
    if (addr.pointed.sa_family.toInt() != AF_INET) return null
    val sin = addr.reinterpret<sockaddr_in>().pointed
    memScoped {
        val buffer = allocArray<ByteVar>(INET_ADDRSTRLEN)
        val result = inet_ntop(
            AF_INET,
            sin.sin_addr.ptr,
            buffer,
            INET_ADDRSTRLEN.toUInt(),
        )
        return result?.toKString()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun readMacAddress(addr: CPointer<sockaddr>): String? {
    if (addr.pointed.sa_family.toInt() != AF_LINK) return null
    val len = addr.pointed.sa_len.toInt()
    if (len < 8) return null

    val bytes = addr.reinterpret<ByteVar>().readBytes(len)

    val nameLen = bytes[5].toInt() and 0xFF
    val macLen = bytes[6].toInt() and 0xFF
    if (macLen <= 0) return null

    val macStart = 8 + nameLen
    if (macStart + macLen > bytes.size) return null

    val macBytes = bytes.copyOfRange(macStart, (macStart + macLen).coerceAtMost(bytes.size))
    if (macBytes.isEmpty() || macBytes.all { it == 0.toByte() }) return null

    return macBytes.take(6).joinToString(":") { byte ->
        (byte.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase()
    }
}

actual fun connectedWifiFrequencyMhz(): Int? = null
