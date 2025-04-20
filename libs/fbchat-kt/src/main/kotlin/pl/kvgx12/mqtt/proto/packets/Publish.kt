package pl.kvgx12.mqtt.proto.packets

import io.ktor.utils.io.core.*
import kotlinx.io.Sink
import pl.kvgx12.mqtt.proto.MQTTPacketType
import pl.kvgx12.mqtt.proto.MQTTQoS

class MQTTPublish(
    messageId: Int,
    val topic: String,
    val payload: ByteArray,
    duplicate: Boolean = false,
    qos: MQTTQoS = MQTTQoS.FireAndForget,
    retain: Boolean = false,
) : MQTTPacketWithMessageId(
    messageId,
    duplicate = duplicate,
    qos = qos,
    retain = retain,
) {
    override val type: MQTTPacketType get() = MQTTPacketType.PUBLISH
    override val variableHeaderSize: Int
        get() = super.variableHeaderSize + 2 + topic.length

    override val payloadSize: Int get() = payload.size

    override fun Sink.encodeVariableHeader() {
        writeMqttString(topic)
        encodeMessageId()
    }

    override fun Sink.encodePayload() {
        writeFully(payload)
    }
}

class MQTTPublishAck(
    messageId: Int,
) : MQTTPacketWithRequiredMessageId(messageId) {
    override val type: MQTTPacketType get() = MQTTPacketType.PUBACK
}

class MQTTPublishReceived(
    messageId: Int,
) : MQTTPacketWithRequiredMessageId(messageId) {
    override val type: MQTTPacketType get() = MQTTPacketType.PUBREC
}

class MQTTPublishRelease(
    messageId: Int,
) : MQTTPacketWithRequiredMessageId(messageId, qos = MQTTQoS.Acknowledged) {
    override val type: MQTTPacketType get() = MQTTPacketType.PUBREL
}

class MQTTPublishComplete(
    messageId: Int,
) : MQTTPacketWithRequiredMessageId(messageId) {
    override val type: MQTTPacketType get() = MQTTPacketType.PUBCOMP
}
