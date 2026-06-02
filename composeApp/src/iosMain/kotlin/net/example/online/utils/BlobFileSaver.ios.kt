package net.example.online.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

actual class BlobFileSaver actual constructor() {

    @OptIn(ExperimentalForeignApi::class)
    actual fun saveBase64File(
        base64Data: String,
        mimeType: String,
        fileName: String,
        onResult: (BlobSaveResult) -> Unit,
    ) {
        val uniqueFileName = generateUniqueBlobFileName(fileName)
        val pureBase64 = extractPureBase64(base64Data)

        val data = NSData.create(
            base64EncodedString = pureBase64,
            options = NSDataBase64DecodingIgnoreUnknownCharacters,
        )
        if (data == null) {
            onResult(BlobSaveResult.Error("Ошибка декодирования файла"))
            return
        }

        val documentsUrl = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        val fileUrl = documentsUrl?.URLByAppendingPathComponent(uniqueFileName)
        if (fileUrl == null) {
            onResult(BlobSaveResult.Error("Не удалось получить каталог Documents"))
            return
        }

        val written = data.writeToURL(fileUrl, atomically = true)
        if (written) {
            onResult(BlobSaveResult.Success(uniqueFileName, fileUrl.path))
        } else {
            onResult(BlobSaveResult.Error("Ошибка сохранения файла"))
        }
    }
}
