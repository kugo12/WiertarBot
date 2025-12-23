package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.NestedJsonListSerializer
import pl.kvgx12.telegram.data.TReactionType

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#setmessagereaction)
 */
@Serializable
data class TSetMessageReactionRequest(
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("message_id")
    val messageId: Long,
    val reaction: @Serializable(NestedJsonListSerializer::class) List<TReactionType>,
    @SerialName("is_big")
    val isBig: Boolean = false,
)
