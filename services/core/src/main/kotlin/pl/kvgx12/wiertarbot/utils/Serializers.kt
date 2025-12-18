package pl.kvgx12.wiertarbot.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.springframework.ai.chat.messages.ToolResponseMessage

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
