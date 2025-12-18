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
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.connector.SendResponse
import java.util.*
import org.springframework.ai.chat.messages.UserMessage as SpringUserMessage


@Serializable
data class ResponseData(
    val text: String,
    val mentions: List<Mention>
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
)


class AIService(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val contextHolder: ContextHolder,
) {
    suspend fun afterSuccessfulSend(result: GenerationResult, sendResponse: SendResponse) {
        result.assMessage.metadata[METADATA_MESSAGE_ID] = sendResponse.messageId
        chatMemory.add(result.conversationId, result.assMessage)
        chatMemory.applyRetention(result.conversationId)
    }

    // FIXME: handle invalid response + invalid mentions
    // TODO: fetch replyToId message if it is not in conversation
    // TODO: thread name cache
    // TODO: handle long messages "safely"?
    // TODO: tool calls

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun generate(
        event: MessageEvent,
        message: String,
        conversationId: String? = null,
    ): GenerationResult {
        val author = contextHolder[event.connectorInfo.connectorType]
            .fetchThread(event.authorId)!!

        val conversationId = conversationId ?: getConversationId(event)

        val metadata = UserMessage.Metadata(
            authorId = event.authorId,
            authorName = author.name,
            threadId = event.threadId,
            messageId = event.messageId,
            replyToMessageId = event.replyToId,
            botId = event.connectorInfo.botId,
            mentions = event.mentionsList.map {
                ResponseData.Mention(it.threadId, it.offset, it.length)
            },
            hasAttachments = event.attachmentsList.isNotEmpty(),
        )

        val message = SpringUserMessage.builder()
            .text(Json.encodeToString(UserMessage(message, metadata)))
            .metadata(mapOf(METADATA_MESSAGE_ID to event.messageId))
            .build()

        chatMemory.add(conversationId, message)

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

        val text = textSb.toString()

        val data = ResponseData.converter.convert(text)
        return GenerationResult(
            conversationId,
            data,
            AssistantMessage(text),
        )
    }

    private suspend fun getConversationId(event: MessageEvent): String =
        findConversationId(event)
            ?: "${event.connectorInfo.connectorType}-${event.threadId}-${UUID.randomUUID()}"

    suspend fun findConversationId(event: MessageEvent): String? = when {
        event.hasReplyToId() && event.replyToId.isNotBlank() ->
            chatMemory.findConversationIdByMessageId(event.replyToId)

        else -> null
    }
}
