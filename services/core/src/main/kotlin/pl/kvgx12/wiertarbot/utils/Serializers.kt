package pl.kvgx12.wiertarbot.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.ToolResponseMessage
import pl.kvgx12.wiertarbot.proto.ThreadData
import pl.kvgx12.wiertarbot.proto.threadData
import pl.kvgx12.wiertarbot.proto.threadParticipant

object ToolCallSerializer : KSerializer<AssistantMessage.ToolCall> {
    @Serializable
    private class ToolCallSurrogate(
        val id: String,
        val type: String,
        val name: String,
        val arguments: String,
    )

    override val descriptor: SerialDescriptor = SerialDescriptor(
        "org.springframework.ai.chat.messages.AssistantMessage.ToolCall",
        ToolCallSurrogate.serializer().descriptor
    )

    val listSerializer = ListSerializer(this)

    override fun serialize(encoder: Encoder, value: AssistantMessage.ToolCall) {
        encoder.encodeSerializableValue(
            ToolCallSurrogate.serializer(), ToolCallSurrogate(
                id = value.id,
                type = value.type,
                name = value.name,
                arguments = value.arguments,
            )
        )
    }

    override fun deserialize(decoder: Decoder): AssistantMessage.ToolCall {
        val surrogate = decoder.decodeSerializableValue(ToolCallSurrogate.serializer())

        return AssistantMessage.ToolCall(surrogate.id, surrogate.type, surrogate.name, surrogate.arguments)
    }
}

object ToolResponseSerializer : KSerializer<ToolResponseMessage.ToolResponse> {
    @Serializable
    private class ToolResponseSurrogate(
        val id: String,
        val name: String,
        val responseData: String,
    )

    override val descriptor: SerialDescriptor = SerialDescriptor(
        "org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse",
        ToolResponseSurrogate.serializer().descriptor
    )

    val listSerializer = ListSerializer(this)

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

object ThreadDataJsonSerializer : KSerializer<ThreadData> {
    override val descriptor: SerialDescriptor = SerialDescriptor(
        "pl.kvgx12.wiertarbot.proto.ThreadData",
        ThreadDataSurrogate.serializer().descriptor
    )

    @Serializable
    private data class ThreadDataSurrogate(
        val id: String,
        val name: String,
        val messageCount: Long,
        val participants: List<ThreadParticipantSurrogate>
    )

    @Serializable
    private data class ThreadParticipantSurrogate(
        val id: String,
        val username: String,
        val name: String,
        val customizedName: String,
        val gender: String
    )

    override fun serialize(encoder: Encoder, value: ThreadData) {
        val surrogate = ThreadDataSurrogate(
            id = value.id,
            name = value.name,
            messageCount = value.messageCount,
            participants = value.participantsList.map { p ->
                ThreadParticipantSurrogate(
                    id = p.id,
                    username = p.username,
                    name = p.name,
                    customizedName = p.customizedName,
                    gender = p.gender
                )
            }
        )

        encoder.encodeSerializableValue(ThreadDataSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): ThreadData {
        val surrogate = decoder.decodeSerializableValue(ThreadDataSurrogate.serializer())

        return threadData {
            id = surrogate.id
            name = surrogate.name
            messageCount = surrogate.messageCount
            participants += surrogate.participants.map { p ->
                threadParticipant {
                    id = p.id
                    username = p.username
                    name = p.name
                    customizedName = p.customizedName
                    profilePictureUri = ""
                    gender = p.gender
                }
            }
        }
    }
}
