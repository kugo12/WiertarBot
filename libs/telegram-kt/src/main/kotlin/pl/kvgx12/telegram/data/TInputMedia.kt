package pl.kvgx12.telegram.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#inputmedia)
 */
@Serializable
sealed interface TInputMedia {
    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#inputmediaaudio)
     */
    @Serializable
    @SerialName("audio")
    data class TInputMediaAudio(
        val media: String,
        val caption: String? = null,
        @SerialName("caption_entities")
        val captionEntities: List<TMessageEntity> = emptyList(),
    ) : TInputMedia

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#inputmediaphoto)
     */
    @Serializable
    @SerialName("photo")
    data class TInputMediaPhoto(
        val media: String,
        val caption: String? = null,
        @SerialName("caption_entities")
        val captionEntities: List<TMessageEntity> = emptyList(),
    ) : TInputMedia

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#inputmediavideo)
     */
    @Serializable
    @SerialName("video")
    data class TInputMediaVideo(
        val media: String,
        val caption: String? = null,
        @SerialName("caption_entities")
        val captionEntities: List<TMessageEntity> = emptyList(),
    ) : TInputMedia

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#inputmediadocument)
     */
    @Serializable
    @SerialName("document")
    data class TInputMediaDocument(
        val media: String,
        val caption: String? = null,
        @SerialName("caption_entities")
        val captionEntities: List<TMessageEntity> = emptyList(),
    ) : TInputMedia
}
