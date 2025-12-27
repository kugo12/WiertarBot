@file:OptIn(ExperimentalSerializationApi::class)

package pl.kvgx12.wiertarbot.entities

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.json.Json
import org.springframework.ai.chat.messages.*
import org.springframework.ai.content.MediaContent
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import pl.kvgx12.wiertarbot.utils.ToolCallSerializer
import pl.kvgx12.wiertarbot.utils.ToolResponseSerializer
import java.time.LocalDateTime
import kotlin.io.encoding.Base64

@Serializable
data class AssistantMessageSurrogate(
    val text: String,
    val toolCalls: List<@Serializable(with = ToolCallSerializer::class) AssistantMessage.ToolCall> = emptyList(),
    val thoughtSignatures: List<@Serializable(with = ByteArrayAsBase64Serializer::class) ByteArray> = emptyList(),
)

@Table(name = "ai_message")
class AIMessage(
    @Id
    val id: Long? = null,
    val conversationId: String,
    val messageType: String,
    val content: String,
    val media: MediaWrapper?,
    val messageId: String,
    val createdAt: LocalDateTime,
) {
    fun toMessage(): Message {
        val metadata = buildMap<String, Any> {
            put(METADATA_CREATED_AT, createdAt)
            put(METADATA_MESSAGE_ID, messageId)
        }

        return when (MessageType.fromValue(messageType)) {
            MessageType.USER -> UserMessage.builder()
                .text(content)
                .let { media?.items?.let { m -> it.media(m) } ?: it }
                .metadata(metadata)
                .build()

            MessageType.ASSISTANT -> {
                val surrogate = try {
                    Json.decodeFromString<AssistantMessageSurrogate>(content)
                } catch (_: SerializationException) {
                    AssistantMessageSurrogate(content, emptyList(), emptyList())
                }

                AssistantMessage.builder()
                    .content(surrogate.text)
                    .toolCalls(surrogate.toolCalls)
                    .let { media?.items?.let { m -> it.media(m) } ?: it }
                    .build().apply {
                        this.metadata.putAll(metadata)
                        this.metadata[METADATA_THOUGHT_SIGNATURES] = surrogate.thoughtSignatures
                    }
            }

            MessageType.SYSTEM -> SystemMessage.builder()
                .text(content)
                .metadata(metadata)
                .build()

            MessageType.TOOL -> ToolResponseMessage.builder()
                .responses(Json.Default.decodeFromString(ToolResponseSerializer.listSerializer, content))
                .metadata(metadata)
                .build()
        }
    }

    companion object {
        const val METADATA_CREATED_AT = "createdAt"
        const val METADATA_MESSAGE_ID = "messageId"
        const val METADATA_THOUGHT_SIGNATURES = "thoughtSignatures"

        private val cbor = Cbor {
            encodeDefaults = false
        }

        fun Message.toEntity(conversationId: String): AIMessage {
            val messageId = checkNotNull(metadata[METADATA_MESSAGE_ID] as? String)

            return AIMessage(
                conversationId = conversationId,
                messageType = messageType.value,
                content = when (this) {
                    is ToolResponseMessage -> Json.encodeToString(ToolResponseSerializer.listSerializer, responses)
                    is AssistantMessage -> if (toolCalls.isNotEmpty()) {
                        Json.encodeToString(
                            AssistantMessageSurrogate(
                                text ?: "",
                                toolCalls,
                                (metadata[METADATA_THOUGHT_SIGNATURES] as? List<*>)
                                    ?.filterIsInstance<ByteArray>()
                                    ?: emptyList()
                            )
                        )
                    } else {
                        text ?: ""
                    }

                    else -> text ?: ""
                },
                media = when (this) {
                    is MediaContent -> media.ifEmpty { null }?.let { MediaWrapper(it) }
                    else -> null
                },
                messageId = messageId,
                createdAt = (metadata[METADATA_CREATED_AT] as? LocalDateTime) ?: LocalDateTime.now()
            )
        }
    }
}

class ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    override val descriptor = PrimitiveSerialDescriptor("ByteArrayAsBase64", PrimitiveKind.STRING)

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: ByteArray) {
        encoder.encodeString(Base64.encode(value))
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): ByteArray {
        return Base64.decode(decoder.decodeString())
    }
}
