package pl.kvgx12.fbchat.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

internal open class NestedJsonAsByteArrayDeserializer<T : Any>(
    private val serializer: KSerializer<T>
) : KSerializer<T> {
    override val descriptor = ByteArraySerializer().descriptor

    override fun deserialize(decoder: Decoder) =
        (decoder as JsonDecoder).json.decodeFromString(
            serializer,
            decoder.decodeJsonElement().jsonArray.let {
                buildString(it.size) {
                    it.forEach { appendCodePoint(it.jsonPrimitive.int) }
                }
            }
        )

    override fun serialize(encoder: Encoder, value: T) = throw UnsupportedOperationException()
}

internal open class NestedJsonAsStringDeserializer<T : Any>(private val serializer: KSerializer<T>) : KSerializer<T> {
    override val descriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder) = (decoder as JsonDecoder).json.decodeFromString(
        serializer,
        decoder.decodeString()
    )

    override fun serialize(encoder: Encoder, value: T) = throw UnsupportedOperationException()
}
