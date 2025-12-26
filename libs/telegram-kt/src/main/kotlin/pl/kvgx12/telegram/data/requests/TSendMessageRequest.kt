package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.NestedJsonListSerializer
import pl.kvgx12.telegram.data.TMessageEntity
import pl.kvgx12.telegram.data.TReplyParameters

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#sendmessage)
 */
@Serializable
data class TSendMessageRequest(
    @SerialName("chat_id")
    val chatId: String,
    val text: String,
    val entities: @Serializable(NestedJsonListSerializer::class) List<TMessageEntity> = emptyList(),
    @SerialName("reply_parameters")
    val replyParameters: @Serializable(TReplyParameters.Serializer::class) TReplyParameters? = null,
)
