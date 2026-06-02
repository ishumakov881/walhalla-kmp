package net.example.online.webview

import com.russhwolf.settings.Settings

/**
 * Как `SharedPreferences("auth_session")` в appWebView [WebAppInterface].
 */
internal object WebAppAuthStorage {
    private val settings: Settings = Settings()

    private const val KEY_USER_ID = "auth_session.user_id"
    private const val KEY_SESSION_TOKEN = "auth_session.session_token"
    private const val KEY_ISSUED_AT = "auth_session.issued_at"

    fun load(): UserSession? {
        val userId = settings.getStringOrNull(KEY_USER_ID) ?: return null
        val token = settings.getStringOrNull(KEY_SESSION_TOKEN) ?: return null
        if (userId.isEmpty() || token.isEmpty()) return null
        return UserSession(
            userId = userId,
            sessionToken = token,
            issuedAt = settings.getLongOrNull(KEY_ISSUED_AT) ?: 0L,
        )
    }

    fun save(session: UserSession) {
        settings.putString(KEY_USER_ID, session.userId)
        settings.putString(KEY_SESSION_TOKEN, session.sessionToken)
        settings.putLong(KEY_ISSUED_AT, session.issuedAt)
    }

    fun clear() {
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_SESSION_TOKEN)
        settings.remove(KEY_ISSUED_AT)
    }

    private fun Settings.getStringOrNull(key: String): String? =
        if (hasKey(key)) getString(key, "") else null

    private fun Settings.getLongOrNull(key: String): Long? =
        if (hasKey(key)) getLong(key, 0L) else null
}
