package pl.kvgx12.downloadapi.platforms.reddit

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer

@OptIn(ExperimentalSerializationApi::class)
val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    namingStrategy = JsonNamingStrategy.SnakeCase

    serializersModule = SerializersModule {
        contextual(RedditListingSerializer)
        contextual(IsRedditThingSerializer)
    }
}

@Serializable
private data class Generic<T : Any>(
    val kind: String,
    val data: T,
)

object RedditListingSerializer : GenericRedditSerializer<RedditListing>(RedditListing.serializer())
object IsRedditThingSerializer : GenericRedditSerializer<IsRedditThing>(serializer())

open class GenericRedditSerializer<T : RedditObject>(private val dataSerializer: KSerializer<T>) : KSerializer<T> {
    override val descriptor: SerialDescriptor
        get() = serialDescriptor<Generic<RedditObject>>()

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var kind: String? = null
        var data: RedditObject? = null

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> kind = decodeStringElement(descriptor, index)
                1 -> data = decodeSerializableElement(
                    descriptor,
                    index,
                    when (kind) {
                        RedditThingType.LISTING -> serializer<RedditListing>()
                        RedditThingType.MORE -> serializer<RedditMore>()
                        RedditThingType.LINK -> serializer<RedditLink>()
                        RedditThingType.COMMENT -> serializer<RedditComment>()
                        else -> error("$kind is not supported")
                    },
                )

                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index $index")
            }
        }

        data as? T ?: error("Unexpected type $kind")
    }

    override fun serialize(encoder: Encoder, value: T) = throw UnsupportedOperationException()
}
