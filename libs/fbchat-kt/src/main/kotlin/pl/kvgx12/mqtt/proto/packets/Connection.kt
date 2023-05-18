package pl.kvgx12.mqtt.proto.packets

import io.ktor.utils.io.core.*
import pl.kvgx12.mqtt.proto.MQTTConnectReturnCode
import pl.kvgx12.mqtt.proto.MQTTPacketType
import pl.kvgx12.mqtt.proto.MQTTProtocol

class MQTTConnect(
    val keepAlive: Int,
    val isCleanSession: Boolean,
    val clientId: String,
    val username: String? = null,
    val password: String? = null,
    val version: MQTTProtocol = MQTTProtocol.V3_1,
    val will: Nothing? = null,
) : MQTTPacket() {
    override val type: MQTTPacketType get() = MQTTPacketType.CONNECT
    override val variableHeaderSize get() = 6 + version.identifier.length
    override val payloadSize: Int
        get() {
            var size = 2 + clientId.length
            if (username != null) size += 2 + username.length
            if (password != null) size += 2 + password.length

            return size
        }

    override fun BytePacketBuilder.encodePayload() {
        writeMqttString(clientId)
        username?.let(::writeMqttString)
        password?.let(::writeMqttString)
    }

    override fun BytePacketBuilder.encodeVariableHeader() {
        writeMqttString(version.identifier)
        writeByte(version.version.toByte())
        writeByte(  // TODO: will
            (username != null).toInt().shl(7)
                .or((password != null).toInt().shl(6))
                .or(isCleanSession.toInt().shl(1))
                .toByte()
        )
        writeShort(keepAlive.toShort())
    }
}


class MQTTConnectAck(
    val returnCode: MQTTConnectReturnCode
) : MQTTPacket() {
    override val type: MQTTPacketType get() = MQTTPacketType.CONNACK
    override val variableHeaderSize: Int get() = 2

    override fun BytePacketBuilder.encodeVariableHeader() {
        writeByte(0)
        writeByte(returnCode.value.toByte())
    }
}

class MQTTDisconnect : MQTTPacket() {
    override val type: MQTTPacketType get() = MQTTPacketType.DISCONNECT
}
