package net.example.online.push

/**
 * JSON как в старом `Letter.toJSONObject()` (`description` = текст сообщения).
 */
internal object LetterJson {
    private const val KEY_ID = "id"
    private const val KEY_TITLE = "title"
    private const val KEY_TEXT = "description"
    private const val KEY_TIME = "time"
    private const val KEY_UNREAD = "unread"
    private const val KEY_MESSAGES = "messages"

    fun encode(letter: Letter): String = buildString {
        append('{')
        append("\"$KEY_ID\":").appendQuoted(letter.id).append(',')
        append("\"$KEY_TITLE\":").appendQuoted(letter.title).append(',')
        append("\"$KEY_TEXT\":").appendQuoted(letter.text).append(',')
        append("\"$KEY_TIME\":").append(letter.time).append(',')
        append("\"$KEY_UNREAD\":").append(letter.isUnread)
        append('}')
    }

    fun encodeArray(letters: List<Letter>): String =
        letters.joinToString(prefix = "[", postfix = "]", separator = ",") { encode(it) }

    fun decode(json: String, isPrivate: Boolean): Letter? {
        val fields = parseObject(json) ?: return null
        val id = fields[KEY_ID] ?: return null
        return Letter(
            isPrivate = isPrivate,
            id = id,
            title = fields[KEY_TITLE] ?: "",
            text = fields[KEY_TEXT] ?: "",
            time = fields[KEY_TIME]?.toLongOrNull() ?: 0L,
        ).apply {
            setUnread(fields[KEY_UNREAD]?.toBooleanStrictOrNull() ?: true)
        }
    }

    fun decodeArray(jsonArray: String, isPrivate: Boolean): List<Letter> =
        splitObjects(jsonArray).mapNotNull { decode(it, isPrivate) }

    fun wrapPrivateMessages(arrayJson: String): String =
        """{"$KEY_MESSAGES":$arrayJson}"""

    fun unwrapPrivateMessages(jsonObject: String): String? {
        val fields = parseObject(jsonObject) ?: return null
        val raw = fields[KEY_MESSAGES] ?: return null
        return if (raw.startsWith("[")) raw else "[$raw]"
    }

    private fun StringBuilder.appendQuoted(value: String): StringBuilder {
        append('"')
        value.forEach { ch ->
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
        return this
    }

    private fun parseObject(json: String): Map<String, String>? {
        val trimmed = json.trim()
        if (!trimmed.startsWith("{")) return null
        val inner = trimmed.removePrefix("{").removeSuffix("}").trim()
        if (inner.isEmpty()) return emptyMap()
        val result = linkedMapOf<String, String>()
        var i = 0
        while (i < inner.length) {
            val keyStart = inner.indexOf('"', i)
            if (keyStart < 0) break
            val keyEnd = inner.indexOf('"', keyStart + 1)
            if (keyEnd < 0) break
            val key = inner.substring(keyStart + 1, keyEnd)
            val colon = inner.indexOf(':', keyEnd + 1)
            if (colon < 0) break
            var valueStart = colon + 1
            while (valueStart < inner.length && inner[valueStart].isWhitespace()) valueStart++
            val (value, nextIndex) = when (inner.getOrNull(valueStart)) {
                '"' -> readQuoted(inner, valueStart)
                '[' -> readBracketValue(inner, valueStart)
                else -> readToken(inner, valueStart)
            }
            result[key] = value
            val comma = inner.indexOf(',', nextIndex)
            i = if (comma < 0) inner.length else comma + 1
        }
        return result
    }

    private fun readQuoted(source: String, start: Int): Pair<String, Int> {
        val builder = StringBuilder()
        var i = start + 1
        while (i < source.length) {
            when (source[i]) {
                '\\' -> {
                    i++
                    if (i < source.length) {
                        builder.append(
                            when (source[i]) {
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                '"' -> '"'
                                '\\' -> '\\'
                                else -> source[i]
                            },
                        )
                    }
                }
                '"' -> return builder.toString() to i + 1
                else -> builder.append(source[i])
            }
            i++
        }
        return builder.toString() to source.length
    }

    private fun readBracketValue(source: String, start: Int): Pair<String, Int> {
        var depth = 0
        var i = start
        while (i < source.length) {
            when (source[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return source.substring(start, i + 1) to i + 1
                }
            }
            i++
        }
        return source.substring(start) to source.length
    }

    private fun readToken(source: String, start: Int): Pair<String, Int> {
        var end = start
        while (end < source.length && source[end] != ',' && source[end] != '}') end++
        return source.substring(start, end).trim() to end
    }

    private fun splitObjects(arrayJson: String): List<String> {
        val inner = arrayJson.trim().removePrefix("[").removeSuffix("]").trim()
        if (inner.isEmpty()) return emptyList()
        val items = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (index in inner.indices) {
            when (inner[index]) {
                '{' -> {
                    if (depth == 0) start = index
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        items.add(inner.substring(start, index + 1))
                        start = -1
                    }
                }
            }
        }
        return items
    }
}
