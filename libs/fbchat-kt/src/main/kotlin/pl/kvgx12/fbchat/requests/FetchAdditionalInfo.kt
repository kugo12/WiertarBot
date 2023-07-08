package pl.kvgx12.fbchat.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import pl.kvgx12.fbchat.session.Session

internal typealias AdditionalInfoProfiles = Map<String, AdditionalInfoResponse.Profile>

@Serializable
internal data class AdditionalInfoResponse(
    val profiles: AdditionalInfoProfiles,
) {
    @Serializable
    data class Profile(
        val type: String,
        val uri: String,
        val firstName: String? = null,
        @SerialName("is_friend")
        val isFriend: Boolean? = null,
        val gender: String? = null,
        val thumbSrc: String,
        val name: String,
    )
}

internal suspend fun fetchAdditionalInfo(session: Session, ids: List<String>): AdditionalInfoProfiles {
    val response = session.payloadPost(
        "/chat/user_info/",
        buildMap {
            ids.forEachIndexed { index, s ->
                put("ids[$index]", s)
            }
        },
    )

    return Session.json.decodeFromJsonElement<AdditionalInfoResponse>(response)
        .profiles
}
