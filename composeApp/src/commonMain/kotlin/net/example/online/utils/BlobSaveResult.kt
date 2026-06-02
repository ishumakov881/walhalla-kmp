package net.example.online.utils

sealed class BlobSaveResult {
    data class Success(val fileName: String, val filePath: String?) : BlobSaveResult()
    data class Error(val message: String) : BlobSaveResult()
    data class PermissionRequired(
        val permission: String,
        val onPermissionGranted: () -> Unit,
    ) : BlobSaveResult()
}

expect class BlobFileSaver() {
    fun saveBase64File(
        base64Data: String,
        mimeType: String,
        fileName: String,
        onResult: (BlobSaveResult) -> Unit,
    )
}
