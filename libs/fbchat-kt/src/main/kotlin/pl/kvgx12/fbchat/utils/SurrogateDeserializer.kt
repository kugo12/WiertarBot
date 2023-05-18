package pl.kvgx12.fbchat.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer


internal inline fun <reified T : Any, R : Any> surrogateDeserializer(crossinline converter: (T) -> R): KSerializer<R> =
    object : KSerializer<R> {
        private val serializer = serializer<T>()
        override val descriptor = serializer.descriptor

        override fun deserialize(decoder: Decoder): R =
            converter(serializer.deserialize(decoder))

        override fun serialize(encoder: Encoder, value: R) =
            throw UnsupportedOperationException("Serialization is not supported")
    }
