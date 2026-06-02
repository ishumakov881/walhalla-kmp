package dev.walhalla.kmp.device

import android.annotation.SuppressLint
import android.provider.Settings

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.UUID

actual class Installation {

    @SuppressLint("HardwareIds")
    actual fun deviceId(): String {
        val context = DeviceInfo.context ?: return ""
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    }

    @Synchronized
    actual fun installId(): String? {
        val context = DeviceInfo.context ?: return null
        if (cachedInstallId == null) {
            try {
                val installation = File(context.filesDir, UUID_FILENAME)
                cachedInstallId = if (installation.exists()) {
                    readInstallationFile(installation)
                } else {
                    writeInstallationFile(installation)
                }
            } catch (_: Exception) {
                // empty
            }
        }
        return cachedInstallId
    }

    private companion object {
        private const val UUID_FILENAME = "uuid.txt"

        @Volatile
        private var cachedInstallId: String? = null

        @Throws(IOException::class)
        private fun readInstallationFile(installation: File): String {
            RandomAccessFile(installation, "r").use { f ->
                val bytes = ByteArray(f.length().toInt())
                f.readFully(bytes)
                return String(bytes)
            }
        }

        @Throws(IOException::class)
        private fun writeInstallationFile(installation: File): String {
            val id = UUID.randomUUID().toString()
            FileOutputStream(installation).use { out ->
                out.write(id.toByteArray())
            }
            return id
        }
    }
}
