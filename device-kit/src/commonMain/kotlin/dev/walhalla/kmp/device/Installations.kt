package dev.walhalla.kmp.device

/**
 * Удобный доступ без создания нового [Installation] на каждый вызов.
 */
object Installations {
    private val installation: Installation by lazy { Installation() }

    fun deviceId(): String = installation.deviceId()

    /** совпадает с [deviceId]. */
    fun installId(): String? = installation.installId()
}
