package pl.kvgx12.wiertarbot.commands.ai

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import pl.kvgx12.wiertarbot.config.ContextHolder
import pl.kvgx12.wiertarbot.entities.AIMessage.Companion.METADATA_MESSAGE_ID
import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.connector.SendResponse
import pl.kvgx12.wiertarbot.services.CachedContextService
import pl.kvgx12.wiertarbot.utils.getLogger
import java.util.*
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
        val threadId: String,
        val messageId: String,
        val replyToMessageId: String,
        val botId: String,
        val mentions: List<ResponseData.Mention>,
        val hasAttachments: Boolean,
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
) {
    private val log = getLogger()

    suspend fun afterSuccessfulSend(result: GenerationResult, sendResponse: SendResponse) {
        result.assMessage.metadata[METADATA_MESSAGE_ID] = "${result.connectorType.name}-${sendResponse.messageId}"
        chatMemory.add(result.conversationId, result.assMessage)
        chatMemory.applyRetention(result.conversationId)
    }

    // FIXME: handle invalid response
    // TODO: configurable retries?
    // TODO: tool calls
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

        val textSb = StringBuilder()
        chatClient.prompt()
            .messages(chatMemory.get(conversationId))
            .stream()
            .chatResponse()
            .asFlow()
            .flatMapConcat { it.results.asFlow() }
            .map { it.output }
            .collect {
                if (it.text != null) {
                    textSb.append(it.text)
                }
            }

        val text = ResponseData.converter.clean(textSb.toString())

        log.debug("AI Response: {}", text)

        val data = ResponseData.converter.convert(text)
            .fixMentions()

        return GenerationResult(
            conversationId,
            data,
            AssistantMessage(text),
            event.connectorInfo.connectorType,
        )
    }

    private fun generateConversationId(event: MessageEvent): String =
        "${event.connectorInfo.connectorType}-${event.threadId}-${UUID.randomUUID()}"

    suspend fun findConversationId(event: MessageEvent): String? = when {
        event.hasReplyToId() && event.replyToId.isNotBlank() ->
            chatMemory.findConversationIdByMessageId("${event.connectorInfo.connectorType.name}-${event.replyToId}")

        else -> null
    }

    private fun ResponseData.fixMentions(): ResponseData = copy(
        mentions = mentions
            .filter { it.userId.isNotBlank() && it.offset >= 0 && it.offset + 1 < text.length }
            .map {
                if (it.offset + it.length > text.length) {
                    it.copy(length = text.length - it.offset)
                } else {
                    it
                }
            }
    )

    private suspend fun MessageEvent.toUserMessage(message: String, addReplyToToContext: Boolean): List<SpringUserMessage> {
        val authorName = cachedContextService.getThreadParticipant(connectorInfo.connectorType, threadId, authorId)
            ?.name
            ?: "Unknown User"

        val repliedTo = when {
            addReplyToToContext && hasReplyTo() -> replyTo.toUserMessage(replyTo.text, false)
            addReplyToToContext && hasReplyToId() && replyToId.isNotBlank() -> {
                val repliedMessage = contextHolder[connectorInfo.connectorType].fetchRepliedTo(this)

                repliedMessage?.toUserMessage(repliedMessage.text, false) ?: emptyList()
            }

            else -> emptyList()
        }

        val metadata = UserMessage.Metadata(
            authorId = authorId,
            authorName = authorName,
            threadId = threadId,
            messageId = messageId,
            replyToMessageId = replyToId,
            botId = connectorInfo.botId,
            mentions = mentionsList.map {
                ResponseData.Mention(it.threadId, it.offset, it.length)
            },
            hasAttachments = attachmentsList.isNotEmpty(),
        )

        return repliedTo + SpringUserMessage.builder()
            .text(Json.encodeToString(UserMessage(message, metadata)))
            .metadata(mapOf(METADATA_MESSAGE_ID to "${connectorInfo.connectorType.name}-$messageId"))
            .build()

    }
}
