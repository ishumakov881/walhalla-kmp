package dev.walhalla.kmp.device

expect class Installation() {
    fun deviceId(): String
    fun installId(): String?
}
