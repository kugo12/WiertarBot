package pl.kvgx12.mqtt.proto

enum class MQTTProtocol(
    val version: Int,
    val identifier: String,
) {
    V3_1(3, "MQIsdp"),
}
