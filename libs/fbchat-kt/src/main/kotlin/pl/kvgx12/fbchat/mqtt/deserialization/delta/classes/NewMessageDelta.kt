package pl.kvgx12.fbchat.mqtt.deserialization.delta.classes

import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.mqtt.deserialization.delta.ThreadKey
import pl.kvgx12.fbchat.requests.types.GraphQLBlobAttachment
import pl.kvgx12.fbchat.requests.types.GraphQLExtensibleAttachment
import pl.kvgx12.fbchat.requests.types.GraphQLSticker
import pl.kvgx12.fbchat.utils.NestedJsonAsStringDeserializer
import pl.kvgx12.fbchat.utils.surrogateDeserializer

@Serializable
internal data class NewMessageDelta(
    val body: String? = null,
    val messageMetadata: MessageMetadata,
    val data: Data = Data(),
    val attachments: List<MercuryAttachment> = emptyList(),
    val messageReply: MessageReply = MessageReply(),
) {
    @Serializable
    data class MessageMetadata(
        val actorFbId: String,
        val messageId: String,
        val timestamp: String,
        val threadKey: ThreadKey,
        val tags: List<String> = emptyList(),
    )

    @Serializable
    data class MessageReply(
        val replyToMessageId: Id = Id(),
    ) {
        @Serializable
        data class Id(val id: String? = null)
    }

    @Serializable
    data class Data(
        @Serializable(ListPRNGDeserializer::class)
        val prng: List<PRNG> = emptyList(),
    ) {
        @Serializable
        data class PRNG(
            @SerialName("o")
            val offset: Int,
            @SerialName("l")
            val length: Int,
            @SerialName("i")
            val id: String,
        )

        object ListPRNGDeserializer : NestedJsonAsStringDeserializer<List<PRNG>>(ListSerializer(PRNG.serializer()))
    }

    @Serializable
    data class MercuryAttachment(
        val mercury: Mercury? = null,
        val fileSize: Int? = null,
    ) {
        @Serializable
        data class Mercury(
            @SerialName("extensible_attachment")
            val extensibleAttachment: GraphQLExtensibleAttachment? = null,
            @SerialName("sticker_attachment")
            val stickerAttachment: GraphQLSticker? = null,
            @SerialName("blob_attachment")
            val blobAttachment: GraphQLBlobAttachment? = null,
        )

        fun toAttachment(): Attachment? {
            val attachment = if (mercury != null) {
                mercury.extensibleAttachment?.toAttachment()
                    ?: mercury.blobAttachment
                    ?: mercury.stickerAttachment?.toSticker()
            } else {
                null
            }

            return when (attachment) {
                is FileAttachment -> attachment.copy(size = fileSize)
                is VideoAttachment -> attachment.copy(size = fileSize)
                else -> attachment
            }
        }
    }

    fun toMessageData(
        user: UserId = UserId(messageMetadata.actorFbId),
        thread: ThreadId = messageMetadata.threadKey.toThreadId(),
    ): MessageData {
        val attachments = attachments.mapNotNull { it.toAttachment() }

        return MessageData(
            author = user,
            thread = thread,
            id = messageMetadata.messageId,
            createdAt = messageMetadata.timestamp.toLong(),
            text = body,
            mentions = data.prng.map {
                Mention(
                    UserId(it.id),
                    offset = it.offset,
                    length = it.length,
                )
            },
            repliedTo = messageReply.replyToMessageId.id?.let {
                MessageId(thread, it)
            },

            emojiSize = FBTags.emojiSize(messageMetadata.tags),
            isRead = null,
            readBy = emptyList(),
            reactions = emptyMap(),
            sticker = attachments.firstOrNull { it is Sticker } as? Sticker,
            attachments = attachments.filter { it !is UnsentMessage && it !is Sticker },
            quickReplies = emptyList(),
            unsent = attachments.any { it is UnsentMessage },
            forwarded = FBTags.isForwarded(messageMetadata.tags),
        )
    }
}

internal val newMessageDeltaDeserializer = surrogateDeserializer<NewMessageDelta, _> { value ->
    val thread = value.messageMetadata.threadKey.toThreadId()
    val user = UserId(value.messageMetadata.actorFbId)
    val event = ThreadEvent.Message(
        author = user,
        thread = thread,
        timestamp = value.messageMetadata.timestamp.toLong(),
        message = value.toMessageData(user, thread),
    )

    flowOf(event)
}
