package net.example.online.webview

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.example.online.push.PushInbox

/**
 * Сессия ЛК из WebView — порт [WebAppInterface] (appWebView).
 * `userId` сопоставляется с `msg.user` в push; для inbox как `cabinetState.login` в accesspoint.
 */
object WebAppSession {
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    fun restoreFromStorage() {
        _session.value = WebAppAuthStorage.load()
    }

    /** Логин для приватных пушей (= `msg.user`). */
    fun currentLogin(): String? = _session.value?.userId?.takeIf { it.isNotEmpty() }

    fun getUserSessionJson(): String = WebAppSessionJson.encode(_session.value)

    fun onAuthorized(payload: String) {
        try {
            saveSession(WebAppSessionJson.parse(payload))
        } catch (_: Exception) {
            // как в WebAppInterface: невалидный payload игнорируем
        }
    }

    fun onLogout() {
        clearSession()
    }

    private fun saveSession(session: UserSession) {
        WebAppAuthStorage.save(session)
        _session.value = session
        PushInbox.refreshCounts()
    }

    private fun clearSession() {
        WebAppAuthStorage.clear()
        _session.value = null
        PushInbox.refreshCounts()
    }
}
