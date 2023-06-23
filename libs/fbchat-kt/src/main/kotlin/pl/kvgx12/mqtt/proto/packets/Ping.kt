package pl.kvgx12.mqtt.proto.packets

import pl.kvgx12.mqtt.proto.MQTTPacketType

class MQTTPing : MQTTPacket() {
    override val type: MQTTPacketType get() = MQTTPacketType.PINGREQ
}

class MQTTPong : MQTTPacket() {
    override val type: MQTTPacketType get() = MQTTPacketType.PINGRESP
}
