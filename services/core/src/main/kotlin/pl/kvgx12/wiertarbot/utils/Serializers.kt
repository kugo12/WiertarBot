package pl.kvgx12.wiertarbot.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.springframework.ai.chat.messages.ToolResponseMessage
import pl.kvgx12.wiertarbot.proto.ThreadData

object ToolResponseSerializer : KSerializer<ToolResponseMessage.ToolResponse> {
    @Serializable
    private class ToolResponseSurrogate(
        val id: String,
        val name: String,
        val responseData: String,
    )

    val listSerializer = ListSerializer(this)

    override val descriptor: SerialDescriptor = SerialDescriptor(
        "org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse",
        ToolResponseSurrogate.serializer().descriptor
    )

    override fun serialize(encoder: Encoder, value: ToolResponseMessage.ToolResponse) {
        encoder.encodeSerializableValue(
            ToolResponseSurrogate.serializer(), ToolResponseSurrogate(
                id = value.id,
                name = value.name,
                responseData = value.responseData,
            )
        )
    }

    override fun deserialize(decoder: Decoder): ToolResponseMessage.ToolResponse {
        val surrogate = decoder.decodeSerializableValue(ToolResponseSurrogate.serializer())

        return ToolResponseMessage.ToolResponse(surrogate.id, surrogate.name, surrogate.responseData)
    }
}

object ThreadDataSerializer : KSerializer<ThreadData> {
    private val serializer = ByteArraySerializer()
    override val descriptor: SerialDescriptor = SerialDescriptor(
        "pl.kvgx12.wiertarbot.proto.ThreadData",
        serializer.descriptor
    )

    override fun serialize(encoder: Encoder, value: ThreadData) {
        val bytes = value.toByteArray()
        encoder.encodeSerializableValue(serializer, bytes)
    }

    override fun deserialize(decoder: Decoder): ThreadData {
        val bytes = decoder.decodeSerializableValue(serializer)
        return ThreadData.parseFrom(bytes)
    }
}
