package net.example.online.push

import com.multiplatform.webview.web.WebViewNavigator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Синхронизация счётчика с DOM ЛК (`showPushMessagesButton`).
 * Ждём появления функции на странице (SPA), без фиксированной задержки.
 */
object PushWebViewBridge {

    private const val MAX_ATTEMPTS = 60
    private const val RETRY_INTERVAL_MS = 100L

    /**
     * @return `true` если вызов дошёл до `showPushMessagesButton`, иначе функция так и не появилась.
     */
    suspend fun syncPushMessagesButton(
        navigator: WebViewNavigator,
        unreadCount: Int,
    ): Boolean {
        repeat(MAX_ATTEMPTS) {
            if (!coroutineContext.isActive) return false
            when (runSyncScript(navigator, unreadCount)) {
                SyncResult.Ok -> return true
                SyncResult.NotReady -> delay(RETRY_INTERVAL_MS)
                SyncResult.Error -> delay(RETRY_INTERVAL_MS)
            }
        }
        return false
    }

    private enum class SyncResult { Ok, NotReady, Error }

    private suspend fun runSyncScript(navigator: WebViewNavigator, unreadCount: Int): SyncResult {
        val raw = evaluateJavaScript(navigator, buildSyncScript(unreadCount)) ?: return SyncResult.NotReady
        val token = raw.trim().removeSurrounding("\"").lowercase()
        return when (token) {
            "ok" -> SyncResult.Ok
            "missing" -> SyncResult.NotReady
            else -> SyncResult.Error
        }
    }

    private fun buildSyncScript(unreadCount: Int): String =
        """
        (function() {
          try {
            if (typeof showPushMessagesButton === 'function') {
              showPushMessagesButton($unreadCount);
              return 'ok';
            }
            return 'missing';
          } catch (e) {
            return 'error';
          }
        })();
        """.trimIndent()

    private suspend fun evaluateJavaScript(
        navigator: WebViewNavigator,
        script: String,
    ): String? = withTimeoutOrNull(JS_CALLBACK_TIMEOUT_MS) {
        suspendCoroutine { continuation ->
            try {
                navigator.evaluateJavaScript(script) { result ->
                    if (continuation.context.isActive) {
                        continuation.resume(result)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                continuation.resume(null)
            }
        }
    }

    private const val JS_CALLBACK_TIMEOUT_MS = 2_000L
}
