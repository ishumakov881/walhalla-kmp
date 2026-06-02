package net.example.online.utils

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Base64

actual class BlobFileSaver actual constructor() {

    actual fun saveBase64File(
        base64Data: String,
        mimeType: String,
        fileName: String,
        onResult: (BlobSaveResult) -> Unit,
    ) {
        Thread {
            runCatching {
                doSave(base64Data, fileName, onResult)
            }.onFailure { error ->
                val message = "Ошибка сохранения файла: ${error.message ?: error::class.simpleName}"
                postResult(onResult, BlobSaveResult.Error(message))
            }
        }.start()
    }

    private fun doSave(
        base64Data: String,
        fileName: String,
        onResult: (BlobSaveResult) -> Unit,
    ) {
        val pureBase64 = extractPureBase64(base64Data)
        val bytes = Base64.getDecoder().decode(pureBase64)

        val downloadsDir = Paths.get(System.getProperty("user.home"), "Downloads")
        Files.createDirectories(downloadsDir)
        val uniqueName = generateUniqueBlobFileName(sanitizeDesktopFileName(fileName))
        val target = resolveUniquePath(downloadsDir, uniqueName)
        Files.write(target, bytes)

        postResult(onResult, BlobSaveResult.Success(target.fileName.toString(), target.toString()))
    }

    private fun resolveUniquePath(directory: Path, fileName: String): Path {
        var candidate = directory.resolve(fileName)
        if (!Files.exists(candidate)) return candidate

        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var index = 1
        while (Files.exists(candidate)) {
            candidate = directory.resolve("$base ($index)$ext")
            index++
        }
        return candidate
    }

    private fun sanitizeDesktopFileName(fileName: String): String {
        val cleaned = fileName
            .replace('\\', '_')
            .replace('/', '_')
            .replace(':', '_')
            .trim()
            .take(200)
        return cleaned.ifEmpty { "download" }
    }

    private fun postResult(onResult: (BlobSaveResult) -> Unit, result: BlobSaveResult) {
        onResult(result)
    }
}
