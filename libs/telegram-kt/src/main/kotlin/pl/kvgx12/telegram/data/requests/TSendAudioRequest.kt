package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.data.TMessageEntity
import pl.kvgx12.telegram.data.TReplyParameters

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#sendaudio)
 */
@Serializable
data class TSendAudioRequest(
    @SerialName("chat_id")
    val chatId: String,
    val audio: String,
    val caption: String? = null,
    @SerialName("caption_entities")
    val captionEntities: List<TMessageEntity> = emptyList(),
    @SerialName("reply_parameters")
    val replyParameters: TReplyParameters? = null,
)
