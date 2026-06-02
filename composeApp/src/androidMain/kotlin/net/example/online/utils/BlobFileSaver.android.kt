package net.example.online.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.util.Base64InputStream
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import net.example.online.ContextHelper
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

/** Порт [net.example.core.utils.FileSaver] из appWebView / ldscore. */
actual class BlobFileSaver actual constructor() {

    private val context: Context
        get() = ContextHelper.context
            ?: error("Context is not initialized")

    actual fun saveBase64File(
        base64Data: String,
        mimeType: String,
        fileName: String,
        onResult: (BlobSaveResult) -> Unit,
    ) {
        val uniqueFileName = generateUniqueBlobFileName(fileName)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                onResult(
                    BlobSaveResult.PermissionRequired(permission) {
                        executeOldApiSave(base64Data, mimeType, uniqueFileName, onResult)
                    },
                )
                return
            }
            executeOldApiSave(base64Data, mimeType, uniqueFileName, onResult)
        } else {
            executeMediaStorageSave(base64Data, mimeType, uniqueFileName, onResult)
        }
    }

    private fun executeOldApiSave(
        base64Data: String,
        mimeType: String,
        fileName: String,
        onResult: (BlobSaveResult) -> Unit,
    ) {
        Thread {
            try {
                val pureBase64 = extractPureBase64(base64Data)
                val fileBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                val downloadDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadDir, fileName)
                FileOutputStream(file).use { fos ->
                    fos.write(fileBytes)
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf(mimeType),
                    null,
                )
                postResult(onResult, BlobSaveResult.Success(fileName, file.absolutePath))
            } catch (e: Exception) {
                postResult(onResult, BlobSaveResult.Error(blobSaveErrorMessage(e)))
            }
        }.start()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun executeMediaStorageSave(
        base64Data: String,
        mimeType: String,
        fileName: String,
        onResult: (BlobSaveResult) -> Unit,
    ) {
        Thread {
            try {
                val pureBase64 = extractPureBase64(base64Data)
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                var finalPath: String? = null
                uri?.let { targetUri ->
                    resolver.openOutputStream(targetUri)?.use { output ->
                        Base64InputStream(
                            ByteArrayInputStream(pureBase64.toByteArray()),
                            Base64.DEFAULT,
                        ).use { base64Stream ->
                            base64Stream.copyTo(output)
                        }
                    }

                    val projection = arrayOf(MediaStore.MediaColumns.DATA)
                    resolver.query(targetUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex =
                                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                            finalPath = cursor.getString(columnIndex)
                        }
                    }
                }

                postResult(onResult, BlobSaveResult.Success(fileName, finalPath))
            } catch (e: Exception) {
                postResult(onResult, BlobSaveResult.Error(blobSaveErrorMessage(e)))
            }
        }.start()
    }

    private fun blobSaveErrorMessage(e: Exception): String {
        val baseMsg = if (e.message?.contains("Permission denied", ignoreCase = true) == true) {
            "Нет доступа к памяти. Проверьте разрешения."
        } else {
            "Ошибка сохранения файла"
        }
        return "$baseMsg: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
    }

    private fun postResult(onResult: (BlobSaveResult) -> Unit, result: BlobSaveResult) {
        Handler(Looper.getMainLooper()).post {
            onResult(result)
        }
    }
}
