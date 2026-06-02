package net.example.online.utils

internal data class BlobSaveParams(
    val base64Data: String,
    val mimeType: String,
    val fileName: String,
)

internal object BlobSaveParamsParser {

    fun parse(params: String): BlobSaveParams? {
        val json = params.trim()
        if (!json.startsWith("{")) return null
        return try {
            BlobSaveParams(
                base64Data = requireStringField(json, "base64Data"),
                mimeType = optStringField(json, "mimeType") ?: "application/octet-stream",
                fileName = optStringField(json, "fileName") ?: "download",
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun requireStringField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.let(::unescapeJson)
            ?: throw IllegalArgumentException("Missing field '$field'")
    }

    private fun optStringField(json: String, field: String): String? {
        val pattern = """"$field"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.let(::unescapeJson)
    }

    private fun unescapeJson(value: String): String =
        value.replace("\\\"", "\"").replace("\\\\", "\\")
}

internal fun extractPureBase64(base64Data: String): String =
    if (base64Data.contains(",")) base64Data.substringAfter(',') else base64Data

internal fun generateUniqueBlobFileName(fileName: String): String {
    val timeStamp = kotlin.time.Clock.System.now().toEpochMilliseconds().toString()
    val extension = fileName.substringAfterLast('.', "")
    val baseName = fileName.substringBeforeLast('.', fileName)
    return if (extension.isNotEmpty()) {
        "${baseName}_$timeStamp.$extension"
    } else {
        "${fileName}_$timeStamp"
    }
}
