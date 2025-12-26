package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.NestedJsonListSerializer
import pl.kvgx12.telegram.data.TReplyParameters

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#sendmediagroup)
 */
@Serializable
data class TSendMediaGroupRequest<T : TInputMedia>(
    @SerialName("chat_id")
    val chatId: String,
    val media: @Serializable(NestedJsonListSerializer::class) List<T>,
    @SerialName("reply_parameters")
    val replyParameters: @Serializable(TReplyParameters.Serializer::class) TReplyParameters? = null,
)
