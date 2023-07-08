package pl.kvgx12.mqtt.proto

enum class MQTTQoS(val value: Int) {
    FireAndForget(0),
    Acknowledged(1),
    Assured(2),
    ;

    companion object {
        private val types = values()

        fun valueOf(value: Int): MQTTQoS? = types.getOrNull(value)
    }
}
