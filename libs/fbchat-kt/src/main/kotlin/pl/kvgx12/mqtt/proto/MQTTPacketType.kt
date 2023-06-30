package pl.kvgx12.mqtt.proto

enum class MQTTPacketType(val value: Int) {
    CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PINGREQ(12),
    PINGRESP(13),
    DISCONNECT(14),
    ;

    companion object {
        private val types = values()

        fun valueOf(value: Int): MQTTPacketType? = types.getOrNull(value - 1)
    }
}
