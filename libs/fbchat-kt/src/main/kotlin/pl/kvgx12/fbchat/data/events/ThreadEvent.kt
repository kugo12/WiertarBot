package pl.kvgx12.fbchat.data.events

import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.MessageData
import pl.kvgx12.fbchat.data.MessageId
import pl.kvgx12.fbchat.data.ThreadId
import pl.kvgx12.fbchat.data.UserId

@Serializable
sealed interface ThreadEvent : Event {
    val author: UserId?
    val thread: ThreadId

    @Serializable
    sealed interface WithMessage : ThreadEvent {
        val message: MessageData
        override val author: UserId
    }

    @Serializable
    data class Typing(
        override val author: UserId,
        override val thread: ThreadId,
        val status: Boolean,
    ) : ThreadEvent

    @Serializable
    data class Message(
        override val author: UserId,
        override val thread: ThreadId,
        override val message: MessageData,
        val timestamp: Long,
    ) : ThreadEvent, WithMessage

    @Serializable
    data class MessageReply(
        override val author: UserId,
        override val thread: ThreadId,
        override val message: MessageData,
        val repliedTo: MessageData,
    ) : ThreadEvent, WithMessage

    @Serializable
    data class MessageReaction(
        override val author: UserId,
        override val thread: ThreadId,
        val message: MessageId,
        val reaction: String?,
    ) : ThreadEvent

    @Serializable
    data class UnsendMessage(
        override val author: UserId,
        override val thread: ThreadId,
        val message: MessageId,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class PeopleAdded(
        override val author: UserId,
        override val thread: ThreadId,
        val added: List<UserId>,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class PersonRemoved(
        override val author: UserId,
        override val thread: ThreadId,
        val removed: UserId,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class TitleSet(
        override val author: UserId,
        override val thread: ThreadId,
        val title: String?,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class MessagesDelivered(
        override val author: UserId,
        override val thread: ThreadId,
        val messages: List<MessageId>,
        val timestamp: Long
    ) : ThreadEvent

    @Serializable
    data class ColorSet(
        override val author: UserId?,
        override val thread: ThreadId,
        val color: String,
        val timestamp: Long
    ) : ThreadEvent

    @Serializable
    data class EmojiSet(
        override val author: UserId?,
        override val thread: ThreadId,
        val emoji: String,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class NicknameSet(
        override val author: UserId,
        override val thread: ThreadId,
        val subject: UserId,
        val nickname: String?,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class AdminsAdded(
        override val author: UserId,
        override val thread: ThreadId,
        val added: List<UserId>,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class AdminsRemoved(
        override val author: UserId,
        override val thread: ThreadId,
        val removed: List<UserId>,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class ApprovalModeSet(
        override val author: UserId,
        override val thread: ThreadId,
        val requireAdminApproval: Boolean,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class CallStarted(
        override val author: UserId,
        override val thread: ThreadId,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class CallFinished(
        override val author: UserId,
        override val thread: ThreadId,
        val duration: Long,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class CallJoined(
        override val author: UserId,
        override val thread: ThreadId,
        val timestamp: Long,
    ) : ThreadEvent

    @Serializable
    data class ChangeViewerStatus(
        override val author: UserId,
        override val thread: ThreadId,
        val canReply: Boolean,
        val reason: Int,  // TODO
    ) : ThreadEvent
}
