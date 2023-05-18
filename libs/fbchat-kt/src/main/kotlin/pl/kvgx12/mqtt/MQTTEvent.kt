package pl.kvgx12.mqtt

import pl.kvgx12.mqtt.proto.MQTTConnectReturnCode

sealed interface MQTTEvent {
    data class ConnectionAck(val code: MQTTConnectReturnCode) : MQTTEvent
    data class MessageArrived(val topic: String, val payload: ByteArray) : MQTTEvent
}
