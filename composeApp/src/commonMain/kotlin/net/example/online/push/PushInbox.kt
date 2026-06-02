package net.example.online.push

import com.mmk.kmpnotifier.notification.PayloadData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.example.online.webview.WebAppSession
import kotlin.random.Random

object PushInbox {
    private var pendingDialog: ParsedPush? = null

    private val _counts = MutableStateFlow(MessageCountState())
    val counts: StateFlow<MessageCountState> = _counts.asStateFlow()

    private val _pendingOpenInbox = MutableStateFlow(false)
    val pendingOpenInbox: StateFlow<Boolean> = _pendingOpenInbox.asStateFlow()

    fun requestOpenInbox() {
        _pendingOpenInbox.value = true
    }

    fun consumeOpenInboxRequest(): Boolean =
        _pendingOpenInbox.value.also { _pendingOpenInbox.value = false }

    /** Одна строка inbox: письмо + владелец приватного кабинета (null = общая лента). */
    data class InboxEntry(
        val letter: Letter,
        val privateOwnerUserName: String?,
    ) {
        fun snapshot(): InboxEntry = InboxEntry(
            letter = letter.snapshot(),
            privateOwnerUserName = privateOwnerUserName,
        )
    }

    /**
     * Публичные — всем; приватные (`msg.user`) — только для [WebAppSession.currentLogin].
     * Как [CabinetRepository.loadMessages] в accesspoint.
     */
    fun listInboxEntries(): List<InboxEntry> {
        val login = WebAppSession.currentLogin()
        val out = mutableListOf<InboxEntry>()
        val pub = MessagesProfile()
        pub.restorePublicProfile()
        pub.messages.forEach { out.add(InboxEntry(it, null)) }
        if (!login.isNullOrEmpty()) {
            val priv = MessagesProfile()
            priv.restoreUserProfile(login)
            priv.messages.forEach { out.add(InboxEntry(it, login)) }
        }
        out.sortByDescending { it.letter.time }
        return out
    }

    fun deleteLetter(letterId: String, privateOwnerUserName: String?) {
        val profile = MessagesProfile()
        profile.restorePublicProfile()
        if (!privateOwnerUserName.isNullOrEmpty()) {
            profile.restoreUserProfile(privateOwnerUserName)
        }
        profile.deleteMessage(letterId)
        profile.storeAll(privateOwnerUserName?.takeIf { it.isNotEmpty() })
        refreshCounts()
    }

    /** Как [CabinetRepository.restoreMessage] в accesspoint. */
    fun restoreLetter(entry: InboxEntry) {
        val profile = MessagesProfile()
        profile.restorePublicProfile()
        if (!entry.privateOwnerUserName.isNullOrEmpty()) {
            profile.restoreUserProfile(entry.privateOwnerUserName)
        }
        profile.addMessage(entry.letter)
        profile.storeAll(entry.privateOwnerUserName?.takeIf { it.isNotEmpty() })
        refreshCounts()
    }

    fun markLetterRead(letterId: String, privateOwnerUserName: String?) {
        val profile = MessagesProfile()
        profile.restorePublicProfile()
        if (!privateOwnerUserName.isNullOrEmpty()) {
            profile.restoreUserProfile(privateOwnerUserName)
        }
        profile.setMessageRead(letterId)
        profile.storeAll(privateOwnerUserName?.takeIf { it.isNotEmpty() })
        refreshCounts()
    }

    /** Android FCM: одна точка сохранения (см. [LdsFirebaseMessagingService]). */
    fun handleFcmMessage(
        data: Map<String, Any?>,
        notificationTitle: String? = null,
        notificationBody: String? = null,
        sentTimeSec: Long = 0L,
    ) {
        val parsed = PushPayloadParser.parseDataPayload(
            data = data,
            notificationTitle = notificationTitle,
            notificationBody = notificationBody,
            sentTimeSec = sentTimeSec,
        ) ?: return
        persist(parsed)
    }

    /** iOS: одна точка сохранения (см. AppDelegate.savePush). */
    fun handleRemoteNotification(userInfo: Map<String, Any?>, sentTimeSec: Long = 0L) {
        val parsed = PushPayloadParser.parse(
            userInfo = userInfo,
            sentTimeSec = sentTimeSec,
        ) ?: return
        persist(parsed)
    }

    fun handleNotifierPush(title: String?, body: String?, data: PayloadData) {
        val parsed = PushPayloadParser.fromNotifier(title, body, data) ?: return
        persist(parsed)
    }

    fun consumePendingDialog(): ParsedPush? = pendingDialog.also { pendingDialog = null }

    fun refreshCounts() {
        WebAppSession.restoreFromStorage()
        _counts.value = loadVisibleCounts()
    }

    fun test() {
        val x = Random.nextInt(0, 100)
        _counts.value = MessageCountState(x, x)
    }

    fun markRead(messageId: String, userName: String?) {
        markLetterRead(messageId, userName)
    }

    private fun persist(parsed: ParsedPush) {
        val profile = MessagesProfile()
        profile.restorePublicProfile()
        profile.restoreUserProfile(parsed.userName)
        if (profile.addMessage(parsed.letter)) {
            if (!parsed.userName.isNullOrBlank()) {
                PushSettingsStorage.addKnownPrivateUserName(parsed.userName)
            }
            profile.storeAll(parsed.userName)
            pendingDialog = parsed
            refreshCounts()
        }
    }

    private fun loadVisibleCounts(): MessageCountState {
        val entries = listInboxEntries()
        return MessageCountState(
            total = entries.size,
            unread = entries.count { it.letter.isUnread },
        )
    }
}
