package pl.kvgx12.mqtt

import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import pl.kvgx12.fbchat.utils.timerFlow
import pl.kvgx12.mqtt.proto.MQTTQoS
import pl.kvgx12.mqtt.proto.packets.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

class MQTTClient(private val wsSession: WebSocketSession) {
    private val _messageId = AtomicInteger(0)
    private val messageId
        get() = _messageId.updateAndGet {
            if ((it + 1) and 0xFFFF == 0) it + 2 else it + 1
        }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val outgoing = Channel<MQTTPacket>()

    val events = wsSession.incoming.consumeAsFlow()
        .transform { consumeFrame(it) }

    init {
        outgoing
            .consumeAsFlow()
            .onEach { wsSession.send(Frame.Binary(true, it.asBytePacket())) }
            .launchIn(scope)

        timerFlow(15.seconds).onEach {
            outgoing.send(MQTTPing())
        }.cancellable().launchIn(scope)
    }

    private suspend fun FlowCollector<MQTTEvent>.consumeFrame(frame: Frame) {
        when (val message = MQTTPacket(frame.buffer)) {
            is MQTTConnectAck -> emit(MQTTEvent.ConnectionAck(message.returnCode))

            is MQTTSubscribeAck,
            is MQTTPublishAck,
            is MQTTUnsubscribeAck,
            is MQTTPublishComplete,
            is MQTTPong,
            -> {
            }

            is MQTTPublishReceived -> outgoing.send(MQTTPublishRelease(message.messageId))
            is MQTTPublishRelease -> outgoing.send(MQTTPublishComplete(message.messageId))

            is MQTTPublish -> {
                when (message.qos) {
                    MQTTQoS.Acknowledged -> outgoing.send(MQTTPublishAck(message.messageId))
                    MQTTQoS.Assured -> outgoing.send(MQTTPublishReceived(message.messageId))
                    MQTTQoS.FireAndForget -> {}
                }

                emit(MQTTEvent.MessageArrived(message.topic, message.payload))
            }

            is MQTTPing -> outgoing.send(MQTTPong())
            is MQTTDisconnect, is MQTTSubscribe, is MQTTConnect, is MQTTUnsubscribe ->
                error("Unexpected mqtt packet sent from server $message ${message.type}")
        }
    }

    suspend fun publish(topic: String, message: String) {
        outgoing.send(MQTTPublish(1, topic, message.toByteArray()))
    }

    suspend fun subscribe(topics: List<Pair<String, MQTTQoS>>) {
        outgoing.send(MQTTSubscribe(1, topics))
    }

    suspend fun connect(packet: MQTTConnect) {
        wsSession.send(Frame.Binary(true, packet.asBytePacket()))
        wsSession.flush()
    }

// TODO:
//    suspend fun disconnect() {
//    }
}
