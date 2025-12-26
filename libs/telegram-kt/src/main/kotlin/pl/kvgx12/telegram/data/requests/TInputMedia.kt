package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.NestedJsonListSerializer
import pl.kvgx12.telegram.data.TMessageEntity

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#inputmedia)
 */
@Serializable
sealed interface TInputMedia {
    val media: TInputFile
    val caption: String?
    val captionEntities: @Serializable(NestedJsonListSerializer::class) List<TMessageEntity>

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#inputmediaaudio)
     */
    @Serializable
    @SerialName("audio")
    data class Audio(
        override val media: TInputFile,
        override val caption: String? = null,
        @SerialName("caption_entities")
        override val captionEntities: @Serializable(NestedJsonListSerializer::class) List<TMessageEntity> = emptyList(),
    ) : TInputMedia

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#inputmediaphoto)
     */
    @Serializable
    @SerialName("photo")
    data class Photo(
        override val media: TInputFile,
        override val caption: String? = null,
        @SerialName("caption_entities")
        override val captionEntities: @Serializable(NestedJsonListSerializer::class) List<TMessageEntity> = emptyList(),
    ) : TInputMedia

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#inputmediavideo)
     */
    @Serializable
    @SerialName("video")
    data class Video(
        override val media: TInputFile.UrlOrId,
        override val caption: String? = null,
        @SerialName("caption_entities")
        override val captionEntities: @Serializable(NestedJsonListSerializer::class) List<TMessageEntity> = emptyList(),
    ) : TInputMedia

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#inputmediadocument)
     */
    @Serializable
    @SerialName("document")
    data class Document(
        override val media: TInputFile,
        override val caption: String? = null,
        @SerialName("caption_entities")
        override val captionEntities: @Serializable(NestedJsonListSerializer::class) List<TMessageEntity> = emptyList(),
    ) : TInputMedia
}
