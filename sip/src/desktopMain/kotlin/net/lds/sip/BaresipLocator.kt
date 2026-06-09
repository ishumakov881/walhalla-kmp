package net.lds.sip

import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile

internal object BaresipLocator {
    private val windowsExecutableNames = listOf("baresip.exe", "baresip")
    private val defaultWindowsPaths = listOf(
        "C:/msys64/ucrt64/bin/baresip.exe",
        "C:/msys64/mingw64/bin/baresip.exe",
    )

    fun resolve(): Path? {
        System.getenv("BARESIP_EXE")
            ?.takeIf { it.isNotBlank() }
            ?.let { candidate -> pathOf(candidate) }
            ?.takeIf { it.isRegularFile() }
            ?.let { return it }

        System.getenv("BARESIP_HOME")
            ?.takeIf { it.isNotBlank() }
            ?.let { home ->
                windowsExecutableNames
                    .map { pathOf("$home/$it") }
                    .firstOrNull { it.isRegularFile() }
            }
            ?.let { return it }

        defaultWindowsPaths
            .map { pathOf(it) }
            .firstOrNull { it.isRegularFile() }
            ?.let { return it }

        return findOnPath()
    }

    private fun findOnPath(): Path? {
        val pathEnv = System.getenv("PATH") ?: return null
        val extensions = if (isWindows()) {
            (System.getenv("PATHEXT") ?: ".EXE;.CMD;.BAT").split(';')
        } else {
            listOf("")
        }

        for (dir in pathEnv.split(File.pathSeparator)) {
            if (dir.isBlank()) continue
            for (name in windowsExecutableNames) {
                for (ext in extensions) {
                    val candidate = pathOf(dir, name + ext)
                    if (candidate.isRegularFile()) return candidate
                }
            }
        }
        return null
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").contains("windows", ignoreCase = true)

    private fun pathOf(first: String, vararg more: String): Path =
        Path.of(first, *more)
}
