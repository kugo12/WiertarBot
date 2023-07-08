package pl.kvgx12.fbchat.mqtt.deserialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.UserId
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.utils.surrogateDeserializer

@Serializable
private data class OrcaTyping(
    @SerialName("sender_fbid")
    val senderFbId: Long,
    val state: Int,
)

internal val orcaTypingDeserializer = surrogateDeserializer<OrcaTyping, ThreadEvent.Typing> {
    val user = UserId(it.senderFbId.toString())

    ThreadEvent.Typing(user, user, it.state == 1)
}
