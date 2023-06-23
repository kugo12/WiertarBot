package pl.kvgx12.fbchat.data

import kotlinx.serialization.Serializable


@Serializable
data class Mention(
    val user: ThreadId,
    val offset: Int,
    val length: Int,
)

@Serializable
sealed interface Message {
    val thread: Thread
    val id: String
}

@Serializable
data class MessageId(
    override val thread: Thread,
    override val id: String
) : Message

@Serializable
data class MessageSnippet(
    override val thread: Thread,
    override val id: String,
    val author: ThreadId,
    val createdAt: Long?,
    val text: String,
    val matchedKeywords: Map<Int, String>,
) : Message

@Serializable
data class MessageData(
    override val thread: Thread,
    override val id: String,
    val author: ThreadId,
    val createdAt: Long?,
    val text: String?,
    val mentions: List<Mention>,
    val emojiSize: EmojiSize?,
    val isRead: Boolean?,
    val readBy: List<String>,
    val reactions: Map<String, String>,
    val sticker: Sticker?,
    val attachments: List<Attachment>,
    val quickReplies: List<QuickReply>,
    val unsent: Boolean?,
    val repliedTo: Message?,
    val forwarded: Boolean?,
) : Message
