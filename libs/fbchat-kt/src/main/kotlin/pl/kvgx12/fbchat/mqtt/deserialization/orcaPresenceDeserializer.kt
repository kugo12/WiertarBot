package pl.kvgx12.fbchat.mqtt.deserialization

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.ActiveStatus
import pl.kvgx12.fbchat.data.events.Event
import pl.kvgx12.fbchat.utils.surrogateDeserializer

@Serializable
private data class OrcaPresence(
    @SerialName("list_type")
    val listType: String,
    val list: List<Status>
) {
    @Serializable
    data class Status(
        @SerialName("u")
        val userId: Long,
        @SerialName("p")
        val status: Int,
        @SerialName("l")
        val lastActive: Long? = null,
    )
}

internal val orcaPresenceDeserializer = surrogateDeserializer<OrcaPresence, Event.Presence> { value ->
    Event.Presence(
        full = value.listType == "full",
        statuses = value.list.associate {
            it.userId.toString() to ActiveStatus(
                active = it.status == 2 || it.status == 3,
                lastActive = it.lastActive
            )
        }
    )
}
