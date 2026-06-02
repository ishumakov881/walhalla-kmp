package net.example.online.push

class MessagesProfile {
    val messages: MutableList<Letter> = mutableListOf()

    val numMessagesUnread: Int
        get() = messages.count { it.isUnread }

    fun setMessageRead(id: String?) {
        if (id == null) return
        messages.firstOrNull { it.id == id }?.let { message ->
            if (message.isUnread) message.setRead()
        }
    }

    fun deleteMessage(id: String?): Int {
        if (id == null) return messages.size
        messages.removeAll { it.id == id }
        return messages.size
    }

    fun addMessage(message: Letter): Boolean {
        if (messages.any { it.id == message.id }) return false
        messages.add(message)
        messages.sort()
        return true
    }

    fun storeAll(userName: String?) {
        val publicMessages = JSONArrayBuilder()
        val privateMessages = if (userName.isNullOrEmpty()) null else JSONArrayBuilder()

        for (message in messages) {
            if (message.isPrivate) {
                privateMessages?.add(LetterJson.encode(message))
            } else {
                publicMessages.add(LetterJson.encode(message))
            }
        }

        if (privateMessages != null && !userName.isNullOrEmpty()) {
            val wrapped = LetterJson.wrapPrivateMessages(privateMessages.build())
            PushSettingsStorage.setPrivateProfileJson(userName, wrapped)
        }

        PushSettingsStorage.setPublicMessagesJson(publicMessages.build())
    }

    fun restorePublicProfile() {
        val stored = PushSettingsStorage.getPublicMessagesJson() ?: return
        try {
            LetterJson.decodeArray(stored, isPrivate = false).forEach { messages.add(it) }
            messages.sort()
        } catch (_: Exception) {
            PushSettingsStorage.removePublicMessages()
        }
    }

    fun restoreUserProfile(userName: String?) {
        if (userName.isNullOrEmpty()) return
        val stored = PushSettingsStorage.getPrivateProfileJson(userName) ?: return
        try {
            val arrayJson = LetterJson.unwrapPrivateMessages(stored) ?: return
            LetterJson.decodeArray(arrayJson, isPrivate = true).forEach { messages.add(it) }
            messages.sort()
        } catch (_: Exception) {
            PushSettingsStorage.removePrivateProfile(userName)
        }
    }

    private class JSONArrayBuilder {
        private val items = mutableListOf<String>()
        fun add(item: String) {
            items.add(item)
        }

        fun build(): String = items.joinToString(prefix = "[", postfix = "]", separator = ",")
    }
}
