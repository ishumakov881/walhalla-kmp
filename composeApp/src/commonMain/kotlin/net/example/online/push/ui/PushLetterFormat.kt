package net.example.online.push.ui

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


/** Как [formatLetterDateTime] в старом NotificationMessagesScreen. */
internal fun formatPushLetterDateTime(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val dt = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())
    val day = dt.dayOfMonth.toString().padStart(2, '0')
    val month = dt.monthNumber.toString().padStart(2, '0')
    val hour = dt.hour.toString().padStart(2, '0')
    val minute = dt.minute.toString().padStart(2, '0')
    return "$day.$month.${dt.year} $hour:$minute"
}

internal fun stripHtmlForPreview(text: String): String =
    text.replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
