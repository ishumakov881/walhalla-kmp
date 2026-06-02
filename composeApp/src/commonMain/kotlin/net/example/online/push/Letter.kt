package net.example.online.push


class Letter(
    val isPrivate: Boolean,
    val id: String,
    val title: String,
    val text: String,
    val time: Long,
) : Comparable<Letter> {
    var isUnread: Boolean = true
        private set

    fun setRead() {
        isUnread = false
    }

    internal fun setUnread(unread: Boolean) {
        isUnread = unread
    }

    fun snapshot(): Letter = Letter(
        isPrivate = isPrivate,
        id = id,
        title = title,
        text = text,
        time = time,
    ).apply { setUnread(isUnread) }

    override fun compareTo(other: Letter): Int {
        return when {
            other.time == time -> 0
            other.time > time -> 1
            else -> -1
        }
    }
}

data class MessageCountState(
    val total: Int = 0,
    val unread: Int = 0,
)
