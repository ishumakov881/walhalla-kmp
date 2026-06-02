package net.example.online.webview

import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.web.WebViewNavigator
import multiplatform.network.cmptoast.showToast
import net.example.online.permissions.BlobSaveStoragePermission
import net.example.online.utils.BlobFileSaver
import net.example.online.utils.BlobSaveParams
import net.example.online.utils.BlobSaveParamsParser
import net.example.online.utils.BlobSaveResult

/**
 * `kmpJsBridge.callNative("saveBlob", JSON.stringify({ base64Data, mimeType, fileName }), cb)`
 * — порт [WebAppInterface.saveBlob] (appWebView).
 */
class SaveBlobJsHandler(
    private val fileSaver: BlobFileSaver = BlobFileSaver(),
) : IJsMessageHandler {

    override fun methodName(): String = "saveBlob"

    override fun handle(
        message: JsMessage,
        navigator: WebViewNavigator?,
        callback: (String) -> Unit,
    ) {
        val params = BlobSaveParamsParser.parse(message.params)
        if (params == null) {
            callback("""{"ok":false,"error":"invalid_params"}""")
            return
        }

        saveWithPermissionRetry(
            params = params,
            callback = callback,
        )
    }

    private fun saveWithPermissionRetry(
        params: BlobSaveParams,
        callback: (String) -> Unit,
    ) {
        fileSaver.saveBase64File(
            base64Data = params.base64Data,
            mimeType = params.mimeType,
            fileName = params.fileName,
        ) { result ->
            when (result) {
                is BlobSaveResult.PermissionRequired -> {
                    BlobSaveStoragePermission.request {
                        fileSaver.saveBase64File(
                            base64Data = params.base64Data,
                            mimeType = params.mimeType,
                            fileName = params.fileName,
                        ) { retryResult ->
                            dispatchSaveResult(retryResult, callback)
                        }
                    }
                }

                else -> dispatchSaveResult(result, callback)
            }
        }
    }

    private fun dispatchSaveResult(
        result: BlobSaveResult,
        callback: (String) -> Unit,
    ) {
        when (result) {
            is BlobSaveResult.Success -> {
                showToast("Файл сохранен: ${result.fileName}")
                callback("""{"ok":true,"fileName":"${escapeJson(result.fileName)}"}""")
            }

            is BlobSaveResult.Error -> {
                showToast(result.message)
                callback("""{"ok":false,"error":"${escapeJson(result.message)}"}""")
            }

            is BlobSaveResult.PermissionRequired -> {
                showToast("Нет доступа к памяти. Проверьте разрешения.")
                callback("""{"ok":false,"error":"permission_required"}""")
            }
        }
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")
}
