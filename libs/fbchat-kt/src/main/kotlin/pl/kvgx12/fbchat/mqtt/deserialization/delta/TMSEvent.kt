package pl.kvgx12.fbchat.mqtt.deserialization.delta

import kotlinx.serialization.Serializable

@Serializable
internal data class TMSEvent(
    val deltas: List<Delta> = emptyList(),
    val errorCode: String? = null,
    val firstDeltaSeqId: String? = null,
    val syncToken: String? = null,
    val lastIssuedSeqId: String? = null,
)
