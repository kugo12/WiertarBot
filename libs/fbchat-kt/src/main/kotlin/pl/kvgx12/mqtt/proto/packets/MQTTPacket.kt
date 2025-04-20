package pl.kvgx12.mqtt.proto.packets

import io.ktor.utils.io.core.*
import kotlinx.io.Sink
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

    private fun Sink.encodeFixedHeader() {
        writeByte(
            type.value.shl(4)
                .or(duplicate.toInt() shl 3)
                .or(qos.value shl 1)
                .or(retain.toInt())
                .toByte(),
        )
        writeVariableByteInteger(variableHeaderSize + payloadSize)
    }

    open fun Sink.encodeVariableHeader() {}
    open fun Sink.encodePayload() {}

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

    fun Sink.encodeMessageId(always: Boolean = false) {
        if (always || qos != MQTTQoS.FireAndForget) {
            writeShort(messageId.toShort())
        }
    }

    override fun Sink.encodeVariableHeader() {
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

    override fun Sink.encodeVariableHeader() {
        encodeMessageId(true)
    }
}
