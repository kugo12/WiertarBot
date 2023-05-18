package pl.kvgx12.mqtt.proto

enum class MQTTConnectReturnCode(val value: Int) {
    Accepted(0),
    UnacceptableProtocolVersion(1),
    IdentifierRejected(2),
    ServerUnavailable(3),
    BadCredentials(4),
    NotAuthorized(5);

    companion object {
        private val codes = values()

        fun valueOf(value: Int) = codes.getOrNull(value)
    }
}
