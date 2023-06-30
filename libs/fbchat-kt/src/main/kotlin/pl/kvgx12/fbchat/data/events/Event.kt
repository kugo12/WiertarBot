package pl.kvgx12.fbchat.data.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import pl.kvgx12.fbchat.data.ActiveStatus
import pl.kvgx12.fbchat.data.MessageId
import pl.kvgx12.fbchat.data.ThreadId
import pl.kvgx12.fbchat.data.ThreadLocation

@Serializable
sealed interface Event {
    @Serializable
    data class Unknown(val source: String, val payload: JsonElement) : Event

    @Serializable
    object Connected : Event

    @Serializable
    object Resync : Event

    @Serializable
    object NoOp : Event

    @Serializable
    data class Disconnected(val reason: String) : Event

    @Serializable
    data class Presence(
        val statuses: Map<String, ActiveStatus>,
        val full: Boolean,
    ) : Event

    @Serializable
    data class ThreadsRead(
        val author: ThreadId,
        val threads: List<ThreadId>,
        val timestamp: Long,
    ) : Event

    @Serializable
    data class NewThreadInFolder(
        val thread: ThreadId,
        val location: ThreadLocation,
    ) : Event

    @Serializable
    data class UnfetchedThread(
        val thread: ThreadId,
        val message: MessageId?,
    ) : Event

    @Serializable
    data class MarkFoldersSeen(
        val locations: List<ThreadLocation>,
        val timestamp: Long,
    ) : Event
}
