package pl.kvgx12.fbchat.mqtt.deserialization.delta.classes

import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.MessageId
import pl.kvgx12.fbchat.data.ThreadLocation
import pl.kvgx12.fbchat.data.UserId
import pl.kvgx12.fbchat.data.events.Event
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.mqtt.deserialization.delta.ThreadKey
import pl.kvgx12.fbchat.utils.surrogateDeserializer

@Serializable
private data class ThreadName(
    val messageMetadata: NewMessageDelta.MessageMetadata,
    val name: String? = null,
)

internal val threadNameDeserializer = surrogateDeserializer<ThreadName, _> {
    flowOf(
        ThreadEvent.TitleSet(
            author = UserId(it.messageMetadata.actorFbId),
            thread = it.messageMetadata.threadKey.toThreadId(),
            title = it.name?.ifEmpty { null },
            timestamp = it.messageMetadata.timestamp.toLong()
        )
    )
}

@Serializable
private data class ForcedFetch(
    val threadKey: ThreadKey,
    val messageId: String? = null,
)

internal val forcedFetchDeserializer = surrogateDeserializer<ForcedFetch, _> {
    val thread = it.threadKey.toThreadId()

    flowOf(
        Event.UnfetchedThread(
            thread = thread,
            message = it.messageId?.let { MessageId(thread, it) }
        )
    )
}

@Serializable
private data class ThreadFolder(
    val threadKey: ThreadKey,
    val folder: String,
)

internal val threadFolderDeserializer = surrogateDeserializer<ThreadFolder, _> {
    flowOf(
        Event.NewThreadInFolder(
            thread = it.threadKey.toThreadId(),
            location = ThreadLocation.valueOf(it.folder.removePrefix("FOLDER_"))
        )
    )
}

@Serializable
private data class DeliveryReceipt(
    val messageIds: List<String> = emptyList(),
    val actorFbId: String? = null,
    val threadKey: ThreadKey,
    val deliveredWatermarkTimestampMs: String,
)

internal val deliveryReceiptDeserializer = surrogateDeserializer<DeliveryReceipt, _> {
    val thread = it.threadKey.toThreadId()
    val author = it.actorFbId?.let(::UserId) ?: thread as UserId

    flowOf(
        ThreadEvent.MessagesDelivered(
            author = author,
            thread = thread,
            messages = it.messageIds.map { MessageId(thread, it) },
            timestamp = it.deliveredWatermarkTimestampMs.toLong(),
        )
    )
}

@Serializable
private data class MarkFolderSeen(
    val folders: List<String>,
    val timestamp: String,
)

internal val markFolderSeenDeserializer = surrogateDeserializer<MarkFolderSeen, _> {
    flowOf(
        Event.MarkFoldersSeen(
            locations = it.folders.map { ThreadLocation.valueOf(it.removePrefix("FOLDER_")) },
            timestamp = it.timestamp.toLong()
        )
    )
}
