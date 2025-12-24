@file:OptIn(ExperimentalSerializationApi::class)

package pl.kvgx12.wiertarbot.entities

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import org.springframework.ai.chat.messages.*
import org.springframework.ai.content.MediaContent
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import pl.kvgx12.wiertarbot.utils.ToolCallSerializer
import pl.kvgx12.wiertarbot.utils.ToolResponseSerializer
import java.time.LocalDateTime

@Serializable
data class AssistantMessageSurrogate(
    val text: String,
    val toolCalls: List<@Serializable(with = ToolCallSerializer::class) AssistantMessage.ToolCall> = emptyList()
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
                val (text, toolCalls) = try {
                    val surrogate = Json.Default.decodeFromString<AssistantMessageSurrogate>(content)
                    surrogate.text to surrogate.toolCalls
                } catch (e: Exception) {
                    content to emptyList()
                }

                AssistantMessage.builder()
                    .content(text)
                    .toolCalls(toolCalls)
                    .let { media?.items?.let { m -> it.media(m) } ?: it }
                    .build().apply {
                        this.metadata.putAll(metadata)
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
                        Json.encodeToString(AssistantMessageSurrogate(text ?: "", toolCalls))
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
