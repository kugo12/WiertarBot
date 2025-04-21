@file:Suppress("MemberVisibilityCanBePrivate")

package pl.kvgx12.mqtt.proto.packets

import io.ktor.utils.io.core.*
import kotlinx.io.Sink
import pl.kvgx12.mqtt.proto.MQTTConnectReturnCode
import pl.kvgx12.mqtt.proto.MQTTPacketType
import pl.kvgx12.mqtt.proto.MQTTQoS
import java.nio.ByteBuffer

internal fun Boolean.toInt(): Int = if (this) 1 else 0

internal fun Sink.writeVariableByteInteger(value: Int) {
    var x = value
    do {
        val byte = x and 0x7F
        x = x ushr 7
        if (x > 0) {
            writeByte(byte.or(0x80).toByte())
        } else {
            writeByte(byte.toByte())
        }
    } while (x > 0)
}

internal fun Sink.writeMqttString(string: String) {
    writeShort(string.length.toShort())
    append(string)
}

private fun ByteBuffer.consumeVariableByteInteger(): Int {
    var shift = 0
    var value = 0

    do {
        val byte = get().toInt()
        value += byte and 0x7F shl shift
        require(shift <= 21) { "Malformed Variable Byte Integer" }
        shift += 7
    } while (byte < 0)

    return value
}

private inline val Short.uInt get() = toInt() and 0xFFFF
private inline val Byte.uInt get() = toInt() and 0xFF

private fun ByteBuffer.consumeMessageId(required: Boolean = true): Int {
    return if (required) short.uInt else 0
}

private fun ByteBuffer.consumeQoSList() = buildList {
    while (hasRemaining()) {
        val value = get().toUInt().toInt()

        add(
            MQTTQoS.valueOf(value)
                ?: error("Unexpected QoS value: $value"),
        )
    }
}

private fun ByteBuffer.consumeMqttString(): String =
    ByteArray(short.uInt)
        .apply(::get)
        .decodeToString()

private infix fun Int.bit(n: Int): Boolean =
    ushr(n).and(1) == 1

@Suppress("NOTHING_TO_INLINE")
private inline fun ByteBuffer.discardByte() {
    get()
}

@Suppress("CyclomaticComplexMethod")
private fun ByteBuffer.consumePacket(
    type: MQTTPacketType,
    qos: MQTTQoS,
    duplicate: Boolean,
    retain: Boolean,
) = when (type) {
    MQTTPacketType.CONNECT -> TODO()
    MQTTPacketType.CONNACK -> {
        discardByte()
        val code = get().uInt

        MQTTConnectAck(
            MQTTConnectReturnCode.valueOf(code)
                ?: error("Unexpected CONNACK return code: $code"),
        )
    }

    MQTTPacketType.PUBLISH -> {
        val topic = consumeMqttString()
        val messageId = consumeMessageId(qos != MQTTQoS.FireAndForget)

        val payload = ByteArray(remaining())
            .also(this::get)

        MQTTPublish(
            messageId = messageId,
            topic = topic,
            payload = payload,
            duplicate = duplicate,
            qos = qos,
            retain = retain,
        )
    }

    MQTTPacketType.PUBACK -> MQTTPublishAck(consumeMessageId())
    MQTTPacketType.PUBREC -> MQTTPublishReceived(consumeMessageId())
    MQTTPacketType.PUBREL -> MQTTPublishRelease(consumeMessageId())
    MQTTPacketType.PUBCOMP -> MQTTPublishComplete(consumeMessageId())
    MQTTPacketType.SUBSCRIBE -> TODO()
    MQTTPacketType.SUBACK -> MQTTSubscribeAck(
        consumeMessageId(),
        consumeQoSList(),
    )

    MQTTPacketType.UNSUBSCRIBE -> TODO()
    MQTTPacketType.UNSUBACK -> MQTTUnsubscribeAck(consumeMessageId())
    MQTTPacketType.PINGREQ -> MQTTPing()
    MQTTPacketType.PINGRESP -> MQTTPong()
    MQTTPacketType.DISCONNECT -> MQTTDisconnect()
}

fun MQTTPacket(buffer: ByteBuffer): MQTTPacket {
    val fixedHeader = buffer.get().uInt

    val type = MQTTPacketType.valueOf(fixedHeader ushr 4 and 0xF)
        ?: error("Invalid packet type: ${fixedHeader ushr 4 and 0xF}")
    val qos = MQTTQoS.valueOf(fixedHeader ushr 1 and 3)
        ?: error("Invalid QoS value: ${fixedHeader ushr 1 and 3}")

    val retain = fixedHeader bit 0
    val duplicate = fixedHeader bit 3
    val size = buffer.consumeVariableByteInteger()

    require(buffer.remaining() == size) {
        "ByteBuffer.remaining() != expectedc mqtt packet size"
    }

    val packet = buffer.consumePacket(type, qos, duplicate, retain)

    require(!buffer.hasRemaining())

    return packet
}
