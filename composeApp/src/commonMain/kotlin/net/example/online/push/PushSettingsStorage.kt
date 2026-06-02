package net.example.online.push

import com.russhwolf.settings.Settings

/**
 * Аналог `SharedPreferences` `common` / `cabinet` из старого `MessagesProfile`.
 */
internal object PushSettingsStorage {
    private val settings: Settings = Settings()

    /** Как файл `common`, ключ `messages`. */
    private const val KEY_PUBLIC_MESSAGES = "common.messages"

    /** Список логинов, для которых есть приватный inbox (Settings не даёт перечислить ключи `cabinet.*`). */
    private const val KEY_KNOWN_PRIVATE_USERS = "push.known_private_users"

    /** Последняя успешная отправка FCM/APNs-токена: `device_id:fcm_token`. */
    private const val KEY_LAST_SENT_PUSH_TOKEN = "push.last_sent_token"

    private fun privateKey(userName: String) = "cabinet.$userName"

    fun getKnownPrivateUserNames(): List<String> =
        settings.getStringOrNull(KEY_KNOWN_PRIVATE_USERS)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?: emptyList()

    fun addKnownPrivateUserName(name: String) {
        val key = name.trim()
        if (key.isEmpty()) return
        val set = getKnownPrivateUserNames().toMutableSet()
        if (!set.add(key)) return
        settings.putString(KEY_KNOWN_PRIVATE_USERS, set.joinToString(","))
    }

    fun getPublicMessagesJson(): String? = settings.getStringOrNull(KEY_PUBLIC_MESSAGES)

    fun setPublicMessagesJson(json: String) {
        settings.putString(KEY_PUBLIC_MESSAGES, json)
    }

    fun removePublicMessages() {
        settings.remove(KEY_PUBLIC_MESSAGES)
    }

    fun getPrivateProfileJson(userName: String): String? =
        settings.getStringOrNull(privateKey(userName))

    fun setPrivateProfileJson(userName: String, json: String) {
        settings.putString(privateKey(userName), json)
    }

    fun removePrivateProfile(userName: String) {
        settings.remove(privateKey(userName))
    }

    fun getLastSentPushTokenKey(): String? =
        settings.getStringOrNull(KEY_LAST_SENT_PUSH_TOKEN)

    fun setLastSentPushTokenKey(value: String) {
        settings.putString(KEY_LAST_SENT_PUSH_TOKEN, value)
    }
}
