package dev.walhalla.kmp.device

/** Случайный MAC, который Android/iOS часто отдают вместо аппаратного. */
const val DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00"

/**
 * Сеть устройства (порт ldsonline `MACKeeper`).
 * Перед чтением [macAddress] вызовите [obtainMacAddress] — как в `StatsSendWorker.createFields`.
 */
expect object MACKeeper {
    val ipAddress: String
    val defaultGateway: String
    val macAddress: String

    fun is5GHzBandSupported(): Boolean

    fun obtainMacAddress()
}

/** `obtainMacAddress()` + [MACKeeper.macAddress] для `device/stats`. */
fun deviceMacAddressForStats(): String {
    MACKeeper.obtainMacAddress()
    return MACKeeper.macAddress
}

/** Частота Wi‑Fi (MHz) при подключении; для `device/stats` (`freq`). */
expect fun connectedWifiFrequencyMhz(): Int?
