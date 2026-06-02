package net.example.online.utils

fun formatToken(token: String?, maxLength: Int = 20): String? {
    if (token == null) return null
    return if (token.length > maxLength) {
        val firstPart = token.substring(0, 10)
        val lastPart = token.substring(token.length - 3)
        "$firstPart...$lastPart"
    } else {
        token
    }
}