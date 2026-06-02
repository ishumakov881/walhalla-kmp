package net.example.online.webview

internal object WebAppSessionJson {

    fun parse(payload: String): UserSession {
        val json = payload.trim()
        val userId = requireStringField(json, "userId")
        val sessionToken = requireStringField(json, "sessionToken")
        val issuedAt = optLongField(json, "issuedAt") ?: 0L
        return UserSession(userId = userId, sessionToken = sessionToken, issuedAt = issuedAt)
    }

    fun encode(session: UserSession?): String {
        if (session == null) {
            return """{"userId":null,"sessionToken":null,"issuedAt":null}"""
        }
        return buildString {
            append("{\"userId\":\"")
            append(escapeJson(session.userId))
            append("\",\"sessionToken\":\"")
            append(escapeJson(session.sessionToken))
            append("\",\"issuedAt\":")
            append(session.issuedAt)
            append('}')
        }
    }

    private fun requireStringField(json: String, field: String): String {
        val pattern = """"$field"\s*:\s*"((?:\\.|[^"\\])*)"""".toRegex()
        val value = pattern.find(json)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("Missing field '$field'")
        return unescapeJson(value)
    }

    private fun optLongField(json: String, field: String): Long? {
        val pattern = """"$field"\s*:\s*(\d+)""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun unescapeJson(value: String): String =
        value.replace("\\\"", "\"").replace("\\\\", "\\")
}
