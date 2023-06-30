package pl.kvgx12.fbchat.mqtt.deserialization.delta.payload

import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.MessageId
import pl.kvgx12.fbchat.data.UserId
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.mqtt.deserialization.delta.ThreadKey
import pl.kvgx12.fbchat.mqtt.deserialization.delta.classes.NewMessageDelta

internal object ClientDelta {
    @Serializable
    private data class MessageReaction(
        val threadKey: ThreadKey,
        val messageId: String,
        val action: Int,
        val userId: Long,
        val reaction: String? = null,
    )

    val reactionDeserializer =
        ClientDeltaDeserializer<MessageReaction, _>("deltaMessageReaction") {
            val thread = it.threadKey.toThreadId()

            ThreadEvent.MessageReaction(
                thread = thread,
                message = MessageId(thread, it.messageId),
                author = UserId(it.userId.toString()),
                reaction = if (it.action == 0) it.reaction else null,
            )
        }

    @Serializable
    private data class RecallMessage(
        val threadKey: ThreadKey,
        val senderID: String,
        val messageID: String,
        val deletionTimestamp: Long,
    )

    val recallMessageDeserializer =
        ClientDeltaDeserializer<RecallMessage, _>("deltaRecallMessageData") {
            val thread = it.threadKey.toThreadId()

            ThreadEvent.UnsendMessage(
                author = UserId(it.senderID),
                thread = thread,
                message = MessageId(thread, it.messageID),
                timestamp = it.deletionTimestamp,
            )
        }

    @Serializable
    private data class MessageReply(
        val message: NewMessageDelta,
        val repliedToMessage: NewMessageDelta,
    )

    val messageReplyDeserializer =
        ClientDeltaDeserializer<MessageReply, _>("deltaMessageReply") {
            val user = UserId(it.message.messageMetadata.actorFbId)
            val thread = it.message.messageMetadata.threadKey.toThreadId()

            ThreadEvent.MessageReply(
                author = user,
                thread = thread,
                message = it.message.toMessageData(user, thread),
                repliedTo = it.repliedToMessage.toMessageData(),
            )
        }

    @Serializable
    private data class UpdateThreadTheme(
        val threadKey: ThreadKey,
        val fallbackColor: String,
    )

    val updateThreadTheme =
        ClientDeltaDeserializer<UpdateThreadTheme, _>("deltaUpdateThreadTheme") {
            ThreadEvent.ColorSet(
                thread = it.threadKey.toThreadId(),
                color = it.fallbackColor,
                timestamp = System.currentTimeMillis(),
                author = null,
            )
        }

    @Serializable
    private data class UpdateThreadEmoji(
        val threadKey: ThreadKey,
        val emoji: String,
    )

    val updateThreadEmoji =
        ClientDeltaDeserializer<UpdateThreadEmoji, _>("deltaUpdateThreadEmoji") {
            ThreadEvent.EmojiSet(
                thread = it.threadKey.toThreadId(),
                emoji = it.emoji,
                timestamp = System.currentTimeMillis(),
                author = null,
            )
        }
}
