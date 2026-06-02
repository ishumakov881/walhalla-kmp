package net.example.online.webview

/** Как [WebAppInterface.UserSession] в appWebView. */
data class UserSession(
    val userId: String,
    val sessionToken: String,
    val issuedAt: Long,
)
