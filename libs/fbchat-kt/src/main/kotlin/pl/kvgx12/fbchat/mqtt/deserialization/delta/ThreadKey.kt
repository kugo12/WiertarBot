package pl.kvgx12.fbchat.mqtt.deserialization.delta

import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.GroupId
import pl.kvgx12.fbchat.data.UserId

@Serializable
internal data class ThreadKey(
    val otherUserFbId: String? = null,
    val threadFbId: String? = null,
) {
    fun toThreadId() = when {
        otherUserFbId != null -> UserId(otherUserFbId)
        threadFbId != null -> GroupId(threadFbId)
        else -> error("threadFbId and otherUserFbId is null")
    }

    inline val id get() = otherUserFbId ?: threadFbId
}
