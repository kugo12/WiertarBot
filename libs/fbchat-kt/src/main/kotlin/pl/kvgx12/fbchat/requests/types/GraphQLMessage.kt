package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.utils.NestedJsonAsStringDeserializer
import pl.kvgx12.fbchat.utils.emptyJsonArray

@Serializable
internal data class GraphQLMessage(
    @SerialName("message_id")
    val id: String,
    @SerialName("message_sender")
    val sender: Id,
    @SerialName("timestamp_precise")
    val timestamp: String,
    val unread: Boolean? = null,
    @SerialName("tags_list")
    val tags: List<String> = emptyList(),
    val message: Message,
    @SerialName("message_reactions")
    val reactions: List<Reaction> = emptyList(),
    @SerialName("replied_to_message")
    val repliedTo: RepliedTo = RepliedTo(),
    val sticker: GraphQLSticker? = null,
    @SerialName("extensible_attachment")
    val extensibleAttachments: List<GraphQLExtensibleAttachment> = emptyList(),
    @SerialName("blob_attachments")
    val blobAttachments: List<GraphQLBlobAttachment> = emptyList(),

    @SerialName("platform_xmd_encoded")
    @Serializable(PlatformEncodedDeserializer::class)
    val platformEncoded: PlatformEncoded = PlatformEncoded(),
) {
    object PlatformEncodedDeserializer : NestedJsonAsStringDeserializer<PlatformEncoded>(PlatformEncoded.serializer())

    @Serializable
    data class PlatformEncoded(
        @SerialName("quick_replies")
        @Serializable(QuickRepliesTransformer::class)
        val quickReplies: List<QuickReply> = emptyList(),
    ) {
        object QuickRepliesTransformer : JsonTransformingSerializer<List<QuickReply>>(
            ListSerializer(graphQLQuickReplyDeserializer),
        ) {
            override fun transformDeserialize(element: JsonElement) = when (element) {
                is JsonArray -> element
                is JsonObject -> buildJsonArray { add(element) }
                else -> emptyJsonArray()
            }
        }
    }

    @Serializable
    data class Range(
        val entity: Id? = null,
        val offset: Int,
        val length: Int,
    )

    @Serializable
    data class RepliedTo(val message: GraphQLMessage? = null)

    @Serializable
    data class Message(
        val text: String? = null,
        val ranges: List<Range> = emptyList(),
    )

    @Serializable
    data class Id(val id: String)

    @Serializable
    data class Reaction(
        val user: Id,
        val reaction: String,
    )

    fun toMessageData(thread: Thread): MessageData {
        val attachments = extensibleAttachments
            .mapNotNull { it.toAttachment() }

        return MessageData(
            thread = thread,
            id = id,
            author = UnknownThread(sender.id),
            createdAt = timestamp.toLong(),
            text = message.text,
            reactions = reactions.associate {
                it.user.id to it.reaction
            },
            repliedTo = repliedTo.message?.toMessageData(thread),
            mentions = message.ranges.mapNotNull {
                it.entity?.id?.let { id ->
                    Mention(
                        UserId(id),
                        length = it.length,
                        offset = it.offset,
                    )
                }
            },
            isRead = unread?.not(),
            emojiSize = FBTags.emojiSize(tags),
            forwarded = FBTags.isForwarded(tags),
            sticker = sticker?.toSticker(),
            unsent = attachments.any { it is UnsentMessage },
            attachments = blobAttachments + attachments.filter { it !is UnsentMessage },
            quickReplies = platformEncoded.quickReplies,
            readBy = emptyList(),
        )
    }
}
