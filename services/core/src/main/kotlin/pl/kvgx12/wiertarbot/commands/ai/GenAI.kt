package pl.kvgx12.wiertarbot.commands.ai

import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.reactive.asFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.ai.chat.client.ChatClient
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.ThreadData


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
        val botId: String,
        val mentions: List<ResponseData.Mention>,
        val hasAttachments: Boolean,
    )
}


class GenAI(private val chatClient: ChatClient) {
    suspend fun generate(
        author: ThreadData,
        event: MessageEvent,
        message: String
    ): ResponseData? {
        val metadata = UserMessage.Metadata(
            authorId = event.authorId,
            authorName = author.name,
            threadId = event.threadId,
            messageId = event.externalId,
            botId = event.connectorInfo.botId,
            mentions = event.mentionsList.map {
                ResponseData.Mention(it.threadId, it.offset, it.length)
            },
            hasAttachments = event.attachmentsList.isNotEmpty(),
        )

        val response = chatClient.prompt()
            .user(Json.encodeToString(UserMessage(message, metadata)))
            .stream()
            .content()
            .asFlow()
            .fold(StringBuilder()) { acc, chunk ->
                acc.append(chunk)
            }.toString()

        return if (response.isBlank()) null else ResponseData.converter.convert(response)
    }
}
