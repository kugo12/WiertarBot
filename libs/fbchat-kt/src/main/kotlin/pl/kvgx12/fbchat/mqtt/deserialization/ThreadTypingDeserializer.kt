package pl.kvgx12.fbchat.mqtt.deserialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.UnknownThread
import pl.kvgx12.fbchat.data.UserId
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.utils.surrogateDeserializer

@Serializable
private data class ThreadTyping(
    @SerialName("sender_fbid")
    val senderFbId: Long,
    val thread: String,
    val state: Int,
)

internal val threadTypingDeserializer = surrogateDeserializer<ThreadTyping, ThreadEvent.Typing> {
    ThreadEvent.Typing(
        author = UserId(it.senderFbId.toString()),
        thread = UnknownThread(it.thread),
        status = it.state == 1,
    )
}
