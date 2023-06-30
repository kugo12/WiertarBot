package pl.kvgx12.fbchat.mqtt

import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.mqtt.MQTTClient
import pl.kvgx12.mqtt.proto.MQTTQoS
import pl.kvgx12.mqtt.proto.packets.MQTTConnect
import kotlin.random.Random

internal val defaultQoS = MQTTQoS.Acknowledged
internal val wssDomain = "edge-chat.messenger.com"
internal val wssUrl = "wss://$wssDomain"
internal val topics = arrayOf(
    // Things that happen in chats (e.g.messages)
    "/t_ms",
    // Group typing notifications
    "/thread_typing",
    // Private chat typing notifications
    "/orca_typing_notifications",
    // Active notifications
    "/orca_presence",
    // Other notifications not related to chats (e.g. friend requests)
    "/legacy_web",
)

private fun generateSessionId() = Random.nextLong(0, Long.MAX_VALUE)

private suspend fun Session.connectWebSocket(sessionId: Long) =
    client.webSocketSession {
        url("$wssUrl/chat?sid=$sessionId&region=odn")

        header(
            HttpHeaders.Origin,
            URLBuilder(Session.baseUrl).run {
                encodedPath = ""
                buildString()
            },
        )
    }

private suspend fun MQTTClient.sendConnectionPacket(session: Session, sessionId: Long) {
    val username = buildJsonObject {
        put("u", session.userId)
        put("s", sessionId)
        put("chat_on", true)
        put("fg", false)
        put("d", session.clientId)
        put("aid", 219994525426954)
        putJsonArray("st") {}
        putJsonArray("pm") {}
        put("cp", 3)
        put("ecp", 10)
        put("ct", "websocket")
        put("mqtt_sid", "")
        put("dc", "")
        put("no_auto_fg", true)
        put("gas", null)
        putJsonArray("pack") {}
        put("php_override", "")
        put("p", null)
        put("a", session.client.plugin(UserAgent).agent)
        put("aids", null)
    }.toString()

    connect(
        MQTTConnect(
            15,
            true,
            "mqttwsclient",
            username,
        ),
    )
}

internal suspend fun Session.connectMQTT(): MQTTClient {
    val sessionId = generateSessionId()

    val wsSession = connectWebSocket(sessionId)
    val client = MQTTClient(wsSession)

    client.sendConnectionPacket(this, sessionId)

    return client
}
