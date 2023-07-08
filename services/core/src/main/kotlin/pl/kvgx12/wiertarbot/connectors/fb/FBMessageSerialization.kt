package pl.kvgx12.wiertarbot.connectors.fb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.data.events.ThreadEvent

object FBMessageSerialization {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
    }

    fun serializeMessageEvent(event: ThreadEvent.WithMessage): String =
        json.encodeToString(FBMessage.serializer(), messageEventToFBMessage(event))

    fun deserializeMessageEvent(string: String): FBMessage =
        json.decodeFromString(FBMessage.serializer(), string)

    private fun messageEventToFBMessage(event: ThreadEvent.WithMessage): FBMessage {
        return FBMessage(
            threadId = event.thread.id,
            messageId = event.message.id,
            authorId = event.author.id,
            at = event.message.createdAt?.toDouble(),
            text = event.message.text,
            replyToId = if (event is ThreadEvent.MessageReply) event.repliedTo.id else null,
            forwarded = event.message.forwarded,
            mentions = event.message.mentions.map {
                FBMessage.Mention(
                    threadId = it.user.id,
                    offset = it.offset,
                    length = it.length,
                )
            },
            attachments = event.message.attachments.map {
                serializeAttachment(it)
            },
            stickerId = event.message.sticker?.id,
        )
    }

    private fun serializeAttachment(attachment: Attachment): FBMessage.Attachment {
        val type = attachment::class.simpleName!!
        val id = attachment.id

        return when (attachment) {
            is ImageAttachment -> FBMessage.Attachment(
                id = id,
                type = type,
                originalExtension = attachment.originalExtension,
            )

            is AudioAttachment -> FBMessage.Attachment(
                id = id,
                type = type,
                audioType = attachment.audioType,
                filename = attachment.name,
                url = attachment.url,
            )

            is FileAttachment -> FBMessage.Attachment(
                id = id,
                type = type,
                url = attachment.url,
                name = attachment.name,
                isMalicious = attachment.isMalicious,
            )

            is VideoAttachment -> FBMessage.Attachment(
                id = id,
                type = type,
                previewUrl = attachment.previewUrl,
            )

            else -> FBMessage.Attachment(
                id = id,
                type = type,
            )
        }
    }

//    elif isinstance(att, fbchat.ShareAttachment):
//    out['url'] = att.url
//    out['original_url'] = att.original_url

    @Serializable
    data class FBMessage(
        @SerialName("thread_id")
        val threadId: String,
        @SerialName("message_id")
        val messageId: String,
        @SerialName("author_id")
        val authorId: String,
        val at: Double? = null,
        val text: String? = null,
        @SerialName("reply_to_id")
        val replyToId: String? = null,
        val forwarded: Boolean? = null,
        val mentions: List<Mention> = emptyList(),
        val attachments: List<Attachment> = emptyList(),
        @SerialName("sticker_id")
        val stickerId: String? = null,
    ) {
        @Serializable
        data class Mention(
            @SerialName("thread_id")
            val threadId: String,
            val offset: Int,
            val length: Int,
        )

        @Serializable
        data class Attachment(
            val id: String?,
            val type: String,
            val url: String? = null,
            val name: String? = null, // -_-
            val filename: String? = null,
            @SerialName("is_malicious")
            val isMalicious: Boolean? = null,
            @SerialName("audio_type")
            val audioType: String? = null,
            @SerialName("original_extension")
            val originalExtension: String? = null,
            @SerialName("preview_url")
            val previewUrl: String? = null,
        )
    }
}
