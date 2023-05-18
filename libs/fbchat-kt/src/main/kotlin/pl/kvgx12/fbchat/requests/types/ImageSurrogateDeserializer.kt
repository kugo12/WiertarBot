package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import pl.kvgx12.fbchat.data.Image

internal object ImageSurrogateDeserializer : KSerializer<Image> {
    @Serializable
    private data class ImageSurrogate(
        val uri: String,
        val width: Int? = null,
        val height: Int? = null,
    )

    private val serializer = serializer<ImageSurrogate>()
    override val descriptor: SerialDescriptor
        get() = serializer.descriptor

    override fun deserialize(decoder: Decoder): Image {
        val surrogate = serializer.deserialize(decoder)

        return Image(
            url = surrogate.uri,
            width = surrogate.width,
            height = surrogate.height
        )
    }

    override fun serialize(encoder: Encoder, value: Image) =
        throw UnsupportedOperationException("Serialization is not supported")
}

typealias GraphQLImage = @Serializable(ImageSurrogateDeserializer::class) Image
