package pl.kvgx12.fbchat.mqtt.deserialization.delta.payload

import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import pl.kvgx12.fbchat.data.events.Event
import pl.kvgx12.fbchat.utils.NestedJsonAsByteArrayDeserializer
import pl.kvgx12.fbchat.utils.surrogateDeserializer

@Serializable
private data class ClientPayload(
    @Serializable(PayloadDeserializer::class) val payload: Payload,
) {
    @Serializable
    data class Payload(
        val deltas: List<
            @Serializable(ClientDeltaDeserializer::class)
            Event,
            >,
    )

    object PayloadDeserializer : NestedJsonAsByteArrayDeserializer<Payload>(Payload.serializer())
}

internal val clientPayloadDeserializer = surrogateDeserializer<ClientPayload, _> { data ->
    data.payload.deltas.asFlow()
}

internal object ClientDeltaDeserializer : JsonContentPolymorphicSerializer<Event>(Event::class) {
    private val noOpDeserializer = surrogateDeserializer<Unit, Event> { Event.NoOp }
    private val unknownEventDeserializer = surrogateDeserializer<JsonElement, _> {
        Event.Unknown("/t_ms - ClientPayload", it)
    }

    private val deserializers = listOf(
        ClientDelta.reactionDeserializer,
        ClientDelta.recallMessageDeserializer,
        ClientDelta.messageReplyDeserializer,
        ClientDelta.updateThreadTheme,
        ClientDelta.updateThreadEmoji,
        KeyTransformation(noOpDeserializer, "liveLocationData"), // not needed
        // fields: threadKey // ??
        KeyTransformation(noOpDeserializer, "deltaUpdateThreadAvatarStickerInstructionKey"),
        // fields: threadKey canViewerReply reason actorFbId // user blocked
        KeyTransformation(noOpDeserializer, "deltaChangeViewerStatus"),
    ).associateBy { it.key }

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Event> {
        require(element is JsonObject && element.size == 1) {
            "Expected object with single key, got $element"
        }

        return deserializers[element.keys.first()] ?: unknownEventDeserializer
    }

    inline operator fun JsonObject.contains(element: KeyTransformation<*>) = element.key in this
    inline operator fun <reified T, R : Event> invoke(key: String, crossinline converter: (T) -> R) =
        KeyTransformation(surrogateDeserializer(converter), key)

    class KeyTransformation<T : Any>(
        private val serializer: KSerializer<T>,
        val key: String,
    ) : KSerializer<T> {
        @OptIn(ExperimentalSerializationApi::class)
        override val descriptor = buildClassSerialDescriptor(serializer.descriptor.serialName) {
            element(key, serializer.descriptor)
        }

        override fun deserialize(decoder: Decoder): T = decoder.decodeStructure(descriptor) {
            val index = decodeElementIndex(descriptor)

            require(index != CompositeDecoder.DECODE_DONE)

            decodeSerializableElement(descriptor, index, serializer)
        }

        override fun serialize(encoder: Encoder, value: T) = throw UnsupportedOperationException()
    }
}
