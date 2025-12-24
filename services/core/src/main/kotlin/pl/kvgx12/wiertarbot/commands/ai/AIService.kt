package pl.kvgx12.wiertarbot.commands.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.ToolResponseMessage
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.content.Media
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.model.tool.ToolCallingManager
import org.springframework.core.io.UrlResource
import pl.kvgx12.wiertarbot.config.ContextHolder
import pl.kvgx12.wiertarbot.entities.AIMessage.Companion.METADATA_MESSAGE_ID
import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.ThreadData
import pl.kvgx12.wiertarbot.proto.connector.SendResponse
import pl.kvgx12.wiertarbot.services.CachedContextService
import pl.kvgx12.wiertarbot.utils.ThreadDataJsonSerializer
import pl.kvgx12.wiertarbot.utils.getLogger
import java.util.*
import kotlin.math.abs
import org.springframework.ai.chat.messages.UserMessage as SpringUserMessage


@Serializable
data class ResponseData(
    val text: String,
    val mentions: List<Mention> = listOf()
) {
    @Serializable
    data class Mention(
        val userId: String,
        val offset: Int,
        val length: Int,
    )

    companion object {
        val converter = kotlinxOutputConverter<ResponseData>()
    }
}

@Serializable
data class UserMessage(
    val message: String,
    val metadata: Metadata,
) {

    @Serializable
    data class Metadata(
        val authorId: String,
        val authorName: String,
        val messageId: String,
        val replyToMessageId: String,
        val mentions: List<ResponseData.Mention>,
        val hasAttachments: Boolean,
    )

    @Serializable
    data class TopMetadata(
        val platform: String,
        val botId: String,
        val threadData: @Serializable(ThreadDataJsonSerializer::class) ThreadData?,
    )
}

data class GenerationResult(
    val conversationId: String,
    val data: ResponseData,
    val assMessage: AssistantMessage,
    val connectorType: ConnectorType,
)


class AIService(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val contextHolder: ContextHolder,
    private val cachedContextService: CachedContextService,
    private val toolCallingManager: ToolCallingManager,
    private val chatOptions: ChatOptions,
    private val props: GenAIProperties,
) {
    private val log = getLogger()


    suspend fun afterSuccessfulSend(result: GenerationResult, sendResponse: SendResponse) {
        result.assMessage.metadata[METADATA_MESSAGE_ID] = "${result.connectorType.name}-${sendResponse.messageId}"
        chatMemory.add(result.conversationId, result.assMessage)
        chatMemory.applyRetention(result.conversationId)
    }

    // TODO: consider adding more replies to context?
    // TODO: add command output to context
    // TODO: consider implementing image generation
    // TODO: maybe cache messages with small TTL?

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun generate(
        event: MessageEvent,
        message: String,
        conversationId: String? = null,
    ): GenerationResult {
        val existingConversationId = conversationId
            ?: findConversationId(event)

        val conversationId = existingConversationId ?: generateConversationId(event)

        chatMemory.add(conversationId, event.toUserMessage(message, existingConversationId == null))
        val options = chatOptions.copy<GoogleGenAiChatOptions>().apply {
            toolContext = mapOf("messageEvent" to event)
        }

        var serializationRetries = 0
        var toolCallCount = 0
        while (toolCallCount < props.maxToolCalls) {
            val messages = Prompt(getMessages(event, conversationId), options)

            val response = withContext(Dispatchers.IO) {
                chatClient.prompt(messages)
                    .call()
                    .chatResponse()
            } ?: break

            val assistantMessage = response.result.output

            if (!assistantMessage.hasToolCalls()) {
                val raw = assistantMessage.text ?: ""
                log.debug("AI raw response: {}", raw)

                val text = ResponseData.converter.clean(raw)
                log.debug("AI cleaned response: {}", text)

                val data = try {
                    ResponseData.converter.convert(text)
                        .fixMentions()
                } catch (e: SerializationException) {
                    log.error("Failed to convert AI response to ResponseData, using raw text", e)

                    if (serializationRetries++ >= props.maxSerializationRetries) {
                        log.warn("Max serialization retries reached (${props.maxSerializationRetries}), using raw text")
                        ResponseData(text)
                    } else {
                        log.info("Retrying serialization (${serializationRetries}/${props.maxSerializationRetries})")
                        continue
                    }
                }

                return GenerationResult(
                    conversationId,
                    data,
                    AssistantMessage.builder()
                        .content(text)
                        .media(assistantMessage.media)
                        .build(),
                    event.connectorInfo.connectorType,
                )
            }

            log.info("AI requested tool calls: {}", assistantMessage.toolCalls)
            val toolCallId = UUID.randomUUID()
            assistantMessage.metadata[METADATA_MESSAGE_ID] = "toolcall-$toolCallId"
            chatMemory.add(conversationId, assistantMessage)

            val executionResult = withContext(Dispatchers.IO) {
                toolCallingManager.executeToolCalls(messages, response)
            }

            log.debug("Tool response: {}", executionResult.conversationHistory().last())
            val toolResponseMessage = executionResult.conversationHistory().last() as ToolResponseMessage
            toolResponseMessage.metadata[METADATA_MESSAGE_ID] = "toolresponse-$toolCallId"
            chatMemory.add(conversationId, toolResponseMessage)
            toolCallCount++
        }

        if (toolCallCount >= props.maxToolCalls) {
            log.warn("Max tool calls reached (${props.maxToolCalls}), returning error response")

            val text = "Przekroczono maksymalną liczbę wywołań narzędzi."
            return GenerationResult(
                conversationId,
                ResponseData(text),
                AssistantMessage(text),
                event.connectorInfo.connectorType,
            )
        } else {
            log.warn("Failed to generate a valid response, returning error response")

            val text = "Wystąpił błąd podczas generowania odpowiedzi."
            return GenerationResult(
                conversationId,
                ResponseData(text),
                AssistantMessage(text),
                event.connectorInfo.connectorType,
            )
        }
    }

    private fun generateConversationId(event: MessageEvent): String =
        "${event.connectorInfo.connectorType}-${event.threadId}-${UUID.randomUUID()}"

    suspend fun findConversationId(event: MessageEvent): String? = when {
        event.hasReplyToId() && event.replyToId.isNotBlank() ->
            chatMemory.findConversationIdByMessageId("${event.connectorInfo.connectorType.name}-${event.replyToId}")

        else -> null
    }

    suspend fun getMessages(event: MessageEvent, conversationId: String): List<Message> = buildList {
        try {
            val threadInfo = cachedContextService.getThread(
                event.connectorInfo.connectorType,
                event.threadId,
            )
            val content = Json.encodeToString(
                UserMessage.TopMetadata(
                    platform = event.connectorInfo.connectorType.name,
                    botId = event.connectorInfo.botId,
                    threadData = threadInfo,
                )
            )
            add(SpringUserMessage(content))
        } catch (e: Exception) {
            log.error("Failed to fetch thread info for threadId=${event.threadId}", e)
        }
        addAll(chatMemory.get(conversationId))
    }

    private fun ResponseData.fixMentions(): ResponseData {
        if (mentions.isEmpty()) {
            return this
        }

        log.debug("fixMentions - og: {}", mentions)

        val atPositions = text.mapIndexedNotNull { index, char ->
            if (char == '@') index else null
        }.toMutableList()

        if (atPositions.isEmpty()) {
            log.debug("No @ found in text, removing all mentions")
            return copy(mentions = emptyList())
        }

        val sortedMentions = mentions
            .filter { it.userId.isNotBlank() }
            .sortedBy { it.offset }

        val fixedMentions = sortedMentions.mapNotNull { mention ->
            if (atPositions.isEmpty()) {
                return@mapNotNull null
            }

            val nearestAtIndex = atPositions.indices.minByOrNull {
                abs(atPositions[it] - mention.offset)
            } ?: return@mapNotNull null

            val atPosition = atPositions.removeAt(nearestAtIndex)

            val maxLength = text.length - atPosition
            val length = when {
                mention.length <= 0 -> {
                    val endIndex = text
                        .indexOfAny(charArrayOf(' ', '\n', '\t', ',', '.', '!', '?', ')'), atPosition + 1)
                        .takeIf { it > atPosition } ?: text.length
                    endIndex - atPosition
                }

                mention.length > maxLength -> maxLength
                else -> mention.length
            }

            if (length <= 1) {
                null
            } else {
                mention.copy(offset = atPosition, length = length)
            }
        }

        log.debug("fixMentions - fixed: {}", fixedMentions)

        return copy(mentions = fixedMentions)
    }

    private suspend fun MessageEvent.toUserMessage(message: String, addReplyToToContext: Boolean): List<SpringUserMessage> {
        log.debug("Converting MessageEvent to UserMessage: {}", this)
        val authorName = when (connectorInfo.connectorType) {  // FIXME
            ConnectorType.TELEGRAM -> cachedContextService.getUserNameAsThread(connectorInfo.connectorType, authorId)
                ?: "Unknown User"

            else -> cachedContextService.getThreadParticipant(connectorInfo.connectorType, threadId, authorId)
                ?.name
                ?: "Unknown User"
        }

        val repliedTo = when {
            addReplyToToContext && hasReplyTo() -> replyTo.toUserMessage(replyTo.text, false)
            addReplyToToContext && hasReplyToId() && replyToId.isNotBlank() -> {
                val repliedMessage = contextHolder[connectorInfo.connectorType].fetchRepliedTo(this)

                repliedMessage?.toUserMessage(repliedMessage.text, false) ?: emptyList()
            }

            else -> emptyList()
        }

        val media = attachmentsList.firstOrNull()?.let {
            if (!it.hasImage()) {
                return@let null
            }

            val imageUrl = contextHolder[connectorInfo.connectorType].fetchImageUrl(it.id)
                ?: return@let null

            val mimeType = when (it.image.originalExtension) {
                "png" -> Media.Format.IMAGE_PNG
                "jpg", "jpeg" -> Media.Format.IMAGE_JPEG
                "gif" -> Media.Format.IMAGE_GIF
                "webp" -> Media.Format.IMAGE_WEBP
                else -> Media.Format.IMAGE_PNG
            }

            log.debug("Fetched image URL for attachment {}: {} ({})", it.id, imageUrl, mimeType)

            Media.builder()
                .data(UrlResource(imageUrl))
                .mimeType(mimeType)
                .build()
        }

        val metadata = UserMessage.Metadata(
            authorId = authorId,
            authorName = authorName,
            messageId = messageId,
            replyToMessageId = replyToId,
            mentions = mentionsList.map {
                ResponseData.Mention(it.threadId, it.offset, it.length)
            },
            hasAttachments = attachmentsList.isNotEmpty(),
        )

        return repliedTo + SpringUserMessage.builder()
            .apply { media?.let { media(listOf(it)) } }
            .text(Json.encodeToString(UserMessage(message, metadata)))
            .metadata(mapOf(METADATA_MESSAGE_ID to "${connectorInfo.connectorType.name}-$messageId"))
            .build()

    }
}
