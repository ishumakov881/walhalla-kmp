package dev.walhalla.kmp.device

import java.io.File
import java.util.UUID

actual class Installation {

    actual fun deviceId(): String {
        return readOrCreate(fileFor("device_id"))
    }

    actual fun installId(): String? {
        // Идея: держать installId отдельно, чтобы его можно было удалять при uninstall.
        // На desktop это best-effort и зависит от деинсталлятора (чистит ли он LOCALAPPDATA).
        return readOrCreate(fileFor("install_id"))
    }

    private fun readOrCreate(file: File): String {
        if (file.exists()) {
            file.readText().trim().takeIf { it.isNotEmpty() }?.let { return it }
        }
        val id = UUID.randomUUID().toString()
        file.parentFile?.mkdirs()
        file.writeText(id)
        return id
    }

    private fun fileFor(name: String): File {
        val baseDir = localAppDataDir() ?: run {
            val home = System.getProperty("user.home") ?: "."
            File(home, ".lds-online")
        }
        return File(baseDir, name)
    }
}

private fun localAppDataDir(): File? {
    val localAppData = System.getenv("LOCALAPPDATA")?.trim().orEmpty()
    if (localAppData.isEmpty()) return null
    return File(File(localAppData, "Example"), "webview_ios")
}
