package pl.kvgx12.mqtt.proto.packets

import io.ktor.utils.io.core.*
import pl.kvgx12.mqtt.proto.MQTTPacketType
import pl.kvgx12.mqtt.proto.MQTTQoS

class MQTTSubscribe(
    messageId: Int,
    val topics: List<Pair<String, MQTTQoS>>,
) : MQTTPacketWithRequiredMessageId(messageId, qos = MQTTQoS.Acknowledged) {
    override val type: MQTTPacketType get() = MQTTPacketType.SUBSCRIBE
    override val payloadSize: Int get() = topics.sumOf { 3 + it.first.length }

    override fun BytePacketBuilder.encodePayload() {
        topics.forEach {
            writeMqttString(it.first)
            writeByte(it.second.value.toByte())
        }
    }
}

class MQTTSubscribeAck(
    messageId: Int,
    val grantedQos: List<MQTTQoS>,
) : MQTTPacketWithRequiredMessageId(messageId) {
    override val type: MQTTPacketType get() = MQTTPacketType.SUBACK
    override val payloadSize: Int get() = grantedQos.size

    override fun BytePacketBuilder.encodePayload() {
        grantedQos.forEach {
            writeByte(it.value.toByte())
        }
    }
}

class MQTTUnsubscribe(
    messageId: Int,
    val topics: List<String>,
) : MQTTPacketWithRequiredMessageId(
    messageId,
    qos = MQTTQoS.Acknowledged,
) {
    override val type: MQTTPacketType get() = MQTTPacketType.UNSUBSCRIBE
    override val payloadSize: Int get() = topics.sumOf { 2 + it.length }

    override fun BytePacketBuilder.encodePayload() {
        topics.forEach {
            writeMqttString(it)
        }
    }
}

class MQTTUnsubscribeAck(
    messageId: Int,
) : MQTTPacketWithRequiredMessageId(messageId) {
    override val type: MQTTPacketType get() = MQTTPacketType.UNSUBACK
}
