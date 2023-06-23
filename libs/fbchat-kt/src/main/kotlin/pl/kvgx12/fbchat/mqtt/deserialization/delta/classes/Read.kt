package pl.kvgx12.fbchat.mqtt.deserialization.delta.classes

import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.UserId
import pl.kvgx12.fbchat.data.events.Event
import pl.kvgx12.fbchat.mqtt.deserialization.delta.ThreadKey
import pl.kvgx12.fbchat.utils.surrogateDeserializer


@Serializable
private data class ReadReceipt(
    val actorFbId: String,
    val threadKey: ThreadKey,
    val actionTimestampMs: Long,
)

internal val readReceiptDeserializer = surrogateDeserializer<ReadReceipt, _> {
    flowOf(
        Event.ThreadsRead(
            author = UserId(it.actorFbId),
            threads = listOf(it.threadKey.toThreadId()),
            timestamp = it.actionTimestampMs
        )
    )
}

@Serializable
private data class MarkRead(
    val threadKeys: List<ThreadKey>,
    val actionTimestamp: String,
)

internal val markReadDeserializer = surrogateDeserializer<MarkRead, _> {
    flowOf(
        Event.ThreadsRead(
            author = TODO(),
            threads = it.threadKeys.map(ThreadKey::toThreadId),
            timestamp = it.actionTimestamp.toLong()
        )
    )
}
