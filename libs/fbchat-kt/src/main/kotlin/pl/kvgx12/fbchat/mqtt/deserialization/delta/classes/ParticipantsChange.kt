package pl.kvgx12.fbchat.mqtt.deserialization.delta.classes

import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.UserId
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.utils.surrogateDeserializer

@Serializable
private data class ParticipantsAddedToGroupThread(
    val messageMetadata: NewMessageDelta.MessageMetadata,
    val addedParticipants: List<Participant>,
) {
    @Serializable
    data class Participant(
        val userFbId: String,
    )
}

internal val participantsAddedToGroupThreadDeserializer = surrogateDeserializer<ParticipantsAddedToGroupThread, _> {
    flowOf(
        ThreadEvent.PeopleAdded(
            thread = it.messageMetadata.threadKey.toThreadId(),
            author = UserId(it.messageMetadata.actorFbId),
            timestamp = it.messageMetadata.timestamp.toLong(),
            added = it.addedParticipants.map { participant ->
                UserId(participant.userFbId)
            },
        ),
    )
}

@Serializable
private data class ParticipantLeftGroupThread(
    val messageMetadata: NewMessageDelta.MessageMetadata,
    val leftParticipantFbId: String,
)

internal val participantLeftGroupThreadDeserializer = surrogateDeserializer<ParticipantLeftGroupThread, _> {
    flowOf(
        ThreadEvent.PersonRemoved(
            author = UserId(it.messageMetadata.actorFbId),
            thread = it.messageMetadata.threadKey.toThreadId(),
            timestamp = it.messageMetadata.timestamp.toLong(),
            removed = UserId(it.leftParticipantFbId),
        ),
    )
}
