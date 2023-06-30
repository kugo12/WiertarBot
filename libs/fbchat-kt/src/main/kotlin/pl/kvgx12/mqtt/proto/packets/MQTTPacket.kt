package pl.kvgx12.mqtt.proto.packets

import io.ktor.utils.io.core.*
import pl.kvgx12.mqtt.proto.MQTTPacketType
import pl.kvgx12.mqtt.proto.MQTTQoS

sealed class MQTTPacket(
    val duplicate: Boolean = false,
    val qos: MQTTQoS = MQTTQoS.FireAndForget,
    val retain: Boolean = false,
) {
    abstract val type: MQTTPacketType
    open val payloadSize get() = 0
    open val variableHeaderSize get() = 0

    private fun BytePacketBuilder.encodeFixedHeader() {
        writeByte(
            type.value.shl(4)
                .or(duplicate.toInt() shl 3)
                .or(qos.value shl 1)
                .or(retain.toInt())
                .toByte(),
        )
        writeVariableByteInteger(variableHeaderSize + payloadSize)
    }

    open fun BytePacketBuilder.encodeVariableHeader() {}
    open fun BytePacketBuilder.encodePayload() {}

    fun asBytePacket() = buildPacket {
        encodeFixedHeader()
        encodeVariableHeader()
        encodePayload()
    }
}

sealed class MQTTPacketWithMessageId(
    val messageId: Int,
    duplicate: Boolean = false,
    qos: MQTTQoS = MQTTQoS.FireAndForget,
    retain: Boolean = false,
) : MQTTPacket(duplicate, qos, retain) {
    override val variableHeaderSize: Int get() = if (qos == MQTTQoS.FireAndForget) 0 else 2

    fun BytePacketBuilder.encodeMessageId(always: Boolean = false) {
        if (always || qos != MQTTQoS.FireAndForget) {
            writeShort(messageId.toShort())
        }
    }

    override fun BytePacketBuilder.encodeVariableHeader() {
        encodeMessageId()
    }
}

sealed class MQTTPacketWithRequiredMessageId(
    messageId: Int,
    duplicate: Boolean = false,
    qos: MQTTQoS = MQTTQoS.FireAndForget,
    retain: Boolean = false,
) : MQTTPacketWithMessageId(messageId, duplicate, qos, retain) {
    override val variableHeaderSize: Int get() = 2

    override fun BytePacketBuilder.encodeVariableHeader() {
        encodeMessageId(true)
    }
}
