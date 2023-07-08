package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.utils.surrogateDeserializer
import pl.kvgx12.fbchat.utils.tryAsString
import kotlin.io.path.Path
import kotlin.io.path.extension

internal typealias GraphQLBlobAttachment =
    @Serializable(GraphQLBlobAttachmentDeserializer::class)
    Attachment

internal object GraphQLBlobAttachmentDeserializer : JsonContentPolymorphicSerializer<Attachment>(Attachment::class) {
    @Serializable
    private data class LegacyAttachmentId(
        @SerialName("legacy_attachment_id")
        val id: String? = null,
    )

    private val unknownDeserializer = surrogateDeserializer<LegacyAttachmentId, _> {
        UnknownAttachment(it.id)
    }

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Attachment> {
        require(element is JsonObject)

        return when (element["__typename"].tryAsString()) {
            "MessageImage", "MessageAnimatedImage" -> imageDeserializer
            "MessageVideo" -> videoDeserializer
            "MessageAudio" -> audioDeserializer
            "MessageFile" -> fileDeserializer
            else -> unknownDeserializer
        }
    }

    @Serializable
    private data class File(
        val url: String? = null,
        val size: Int? = null,
        val filename: String? = null,
        @SerialName("is_malicious")
        val isMalicious: Boolean? = null,
        @SerialName("message_file_fbid")
        val id: String? = null,
    )

    private val fileDeserializer = surrogateDeserializer<File, _> {
        FileAttachment(
            id = it.id,
            url = it.url,
            size = it.size,
            isMalicious = it.isMalicious,
            name = it.filename,
        )
    }

    @Serializable
    private data class Audio(
        val filename: String? = null,
        @SerialName("playable_url")
        val url: String? = null,
        @SerialName("playable_duration_in_ms")
        val duration: Long? = null,
        @SerialName("audio_type")
        val type: String? = null,
    )

    private val audioDeserializer = surrogateDeserializer<Audio, _> {
        AudioAttachment(
            id = null, // TODO
            name = it.filename,
            duration = it.duration,
            audioType = it.type,
            url = it.url,
        )
    }

    @Serializable
    private data class Video(
        @SerialName("legacy_attachment_id")
        val id: String? = null,
        @SerialName("playable_url")
        val previewUrl: String? = null,
        @SerialName("original_dimensions")
        val dimensions: Dimension = Dimension(),
        @SerialName("playable_duration_in_ms")
        val duration: Long? = null,
        @SerialName("chat_image")
        val chatImage: GraphQLImage? = null,
        @SerialName("inbox_image")
        val inboxImage: GraphQLImage? = null,
        @SerialName("large_image")
        val largeImage: GraphQLImage? = null,
    )

    @Serializable
    private data class Dimension(
        val width: Int? = null,
        val height: Int? = null,
    )

    private val videoDeserializer = surrogateDeserializer<Video, _> {
        VideoAttachment(
            id = it.id,
            previewUrl = it.previewUrl,
            width = it.dimensions.width,
            height = it.dimensions.height,
            duration = it.duration,
            size = null, // TODO
            previews = listOfNotNull(
                it.chatImage,
                it.inboxImage,
                it.largeImage,
            ),
        )
    }

    @Serializable
    private data class ImageSurrogate(
        @SerialName("legacy_attachment_id")
        val id: String? = null,
        @SerialName("original_extension")
        val extension: String? = null,
        val filename: String? = null,
        @SerialName("original_dimensions")
        val dimension: Dimension = Dimension(),
        @SerialName("__typename")
        val type: String,
        val thumbnail: GraphQLImage? = null,
        val preview: GraphQLImage? = null,
        @SerialName("large_preview")
        val largePreview: GraphQLImage? = null,
        @SerialName("animated_image")
        val animatedImage: GraphQLImage? = null,
        @SerialName("prewview_image")
        val previewImage: GraphQLImage? = null,
    )

    private val imageDeserializer = surrogateDeserializer<ImageSurrogate, _> {
        ImageAttachment(
            id = it.id,
            width = it.dimension.width,
            height = it.dimension.height,
            isAnimated = it.type == "MessageAnimatedImage",
            previews = listOfNotNull(
                it.thumbnail,
                it.previewImage,
                it.preview,
                it.largePreview,
                it.animatedImage,
            ),
            originalExtension = it.extension ?: it.filename?.let(::Path)?.extension,
            name = it.filename,
        )
    }
}
