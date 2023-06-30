package pl.kvgx12.fbchat.mqtt

import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import pl.kvgx12.fbchat.data.events.Event
import pl.kvgx12.fbchat.mqtt.deserialization.delta.TMSEvent
import pl.kvgx12.fbchat.mqtt.deserialization.orcaPresenceDeserializer
import pl.kvgx12.fbchat.mqtt.deserialization.orcaTypingDeserializer
import pl.kvgx12.fbchat.mqtt.deserialization.threadTypingDeserializer
import pl.kvgx12.fbchat.requests.fetchSequenceId
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.mqtt.MQTTClient
import pl.kvgx12.mqtt.MQTTEvent
import pl.kvgx12.mqtt.proto.MQTTConnectReturnCode

class Listener(
    private val session: Session,
) {
    private var syncToken: String? = null

    suspend fun listen(): Flow<Event> {
        val mqtt = session.connectMQTT()

        if (session.sequenceId == null) {
            session.fetchSequenceId()
        }

        mqtt.publishSync()

        return listen(mqtt)
    }

    internal fun listen(client: MQTTClient) =
        client.events.buffer().transform {
            consume(it)
        }

    private suspend fun MQTTClient.publishSync() {
        val topic = if (syncToken == null) "/messenger_sync_create_queue" else "/messenger_sync_get_diffs"
        val message = buildJsonObject {
            put("sync_api_version", 10)
            put("max_deltas_able_to_process", 1000)
            put("delta_batch_size", 500)
            put("encoding", "JSON")
            put("entity_fbid", session.userId)

            if (syncToken != null) {
                put("last_seq_id", session.sequenceId)
                put("sync_token", syncToken)
            } else {
                put("initial_titan_sequence_id", session.sequenceId)
                put("device_params", null)
            }
        }.toString()

        publish(topic, message)
        subscribe(topics.map { it to defaultQoS })
    }

    private suspend fun FlowCollector<Event>.consume(event: MQTTEvent) {
        when (event) {
            is MQTTEvent.ConnectionAck -> if (event.code == MQTTConnectReturnCode.Accepted) {
                emit(Event.Connected)
            } else {
                error("Could not connect ${event.code}")
            }

            is MQTTEvent.MessageArrived -> runCatching {
                consume(event.topic, event.payload.decodeToString())
            }.onFailure {
                log.error("Could not consume ${event.topic} - ${event.payload.decodeToString()}", it)
            }
        }
    }

    private suspend fun FlowCollector<Event>.consume(topic: String, payload: String) {
        when (topic) {
            "/t_ms" -> {
                val event = Session.json.decodeFromString<TMSEvent>(payload)

                if (shouldHandleMs(event)) {
                    event.deltas.forEach { emitAll(it) }
                }
            }

            "/orca_presence" -> emit(
                Session.json.decodeFromString(
                    orcaPresenceDeserializer,
                    payload,
                ),
            )

            "/thread_typing" -> emit(
                Session.json.decodeFromString(
                    threadTypingDeserializer,
                    payload,
                ),
            )

            "/orca_typing_notifications" -> emit(
                Session.json.decodeFromString(
                    orcaTypingDeserializer,
                    payload,
                ),
            )

            else -> emit(Event.Unknown(topic, Session.json.parseToJsonElement(payload)))
        }
    }

    private suspend fun FlowCollector<Event>.shouldHandleMs(payload: TMSEvent) = when {
        payload.syncToken != null && payload.firstDeltaSeqId != null -> {
            syncToken = payload.syncToken
            session.sequenceId = payload.firstDeltaSeqId

            log.debug("Received sync token and sequence id: $syncToken ${session.sequenceId}")

            false
        }

        payload.errorCode != null -> {
            val errorCode = payload.errorCode
            log.warn("Received mqtt error code: $errorCode")

            if (errorCode == "ERROR_QUEUE_NOT_FOUND" || errorCode == "ERROR_QUEUE_OVERFLOW") {
                log.warn("The MQTT listener was disconnected for too long, events may have been lost")
                syncToken = null
                session.sequenceId = null
                emit(Event.Disconnected(errorCode))
            }

            false
        }

        else -> {
            session.sequenceId = payload.lastIssuedSeqId

            true
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Listener::class.java)
    }
}
