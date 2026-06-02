package net.example.online.push

import kotlin.time.Clock

data class ParsedPush(
    val letter: Letter,
    val userName: String?,
)

internal object PushPayloadParser {

    /**
     * Как [MessagingService.onMessageReceived] в accesspoint: data (`msg.*`) в приоритете,
     * title/body из notification — только если в data пусто.
     */
    fun parseDataPayload(
        data: Map<String, Any?>,
        notificationTitle: String? = null,
        notificationBody: String? = null,
        sentTimeSec: Long = 0L,
    ): ParsedPush? {
        if (data.containsKey("commands")) return null

        val flat = flatten(data)
        return buildParsed(flat, notificationTitle, notificationBody, sentTimeSec)
    }

    /** iOS / полный userInfo (aps как запасной источник title/body). */
    fun parse(userInfo: Map<String, Any?>, sentTimeSec: Long = 0L): ParsedPush? {
        if (userInfo.containsKey("commands")) return null
        val flat = flatten(userInfo)
        val notificationTitle = flat["aps.alert.title"] ?: flat["aps.alert"].takeIf {
            flat["aps.alert.body"].isNullOrEmpty()
        }
        val notificationBody = flat["aps.alert.body"]
        return buildParsed(flat, notificationTitle, notificationBody, sentTimeSec)
    }

    fun fromNotifier(title: String?, body: String?, data: Map<String, Any?>): ParsedPush? {
        if (data.containsKey("commands")) return null
        val flat = flatten(data)
        val notificationTitle = title.takeUnless { flat[PushKeys.TITLE].isNullOrEmpty() }
        val notificationBody = body.takeUnless { flat[PushKeys.BODY].isNullOrEmpty() }
        return parseDataPayload(data, notificationTitle, notificationBody)
    }

    private fun buildParsed(
        flat: Map<String, String>,
        notificationTitle: String?,
        notificationBody: String?,
        sentTimeSec: Long,
    ): ParsedPush? {
        val userName = flat[PushKeys.USER_NAME]
        val isPrivate = !userName.isNullOrEmpty()

        var messageId = flat[PushKeys.MESSAGE_ID]
        if (messageId.isNullOrEmpty()) {
            messageId = "${Clock.System.now().toEpochMilliseconds()}"
        }

        var title = flat[PushKeys.TITLE]
        if (title.isNullOrEmpty()) {
            title = notificationTitle ?: ""
        }

        var body = flat[PushKeys.BODY]
        if (body.isNullOrEmpty()) {
            body = notificationBody ?: ""
        }

        val letter = Letter(
            isPrivate = isPrivate,
            id = messageId,
            title = title,
            text = body,
            time = if (sentTimeSec > 0L) sentTimeSec else Clock.System.now().epochSeconds,
        )
        return ParsedPush(letter = letter, userName = userName)
    }

    private fun flatten(userInfo: Map<String, Any?>): Map<String, String> {
        val result = linkedMapOf<String, String>()
        userInfo.forEach { (key, value) ->
            when (key) {
                "aps" -> appendAps(value, result)
                else -> appendScalar(key, value, result)
            }
        }
        return result
    }

    private fun appendAps(value: Any?, result: MutableMap<String, String>) {
        val map = value as? Map<*, *> ?: return
        map.forEach { (rawKey, rawValue) ->
            if (rawKey?.toString() != "alert") return@forEach
            when (rawValue) {
                is Map<*, *> -> {
                    rawValue["title"]?.toString()?.let { result["aps.alert.title"] = it }
                    rawValue["body"]?.toString()?.let { result["aps.alert.body"] = it }
                }
                else -> rawValue?.toString()?.let { result["aps.alert"] = it }
            }
        }
    }

    private fun appendScalar(key: String, value: Any?, result: MutableMap<String, String>) {
        when (value) {
            null -> Unit
            is String -> result[key] = value
            is Number, is Boolean -> result[key] = value.toString()
            is Map<*, *> -> value.forEach { (nestedKey, nestedValue) ->
                appendScalar("$key.${nestedKey}", nestedValue, result)
            }
            else -> result[key] = value.toString()
        }
    }
}
