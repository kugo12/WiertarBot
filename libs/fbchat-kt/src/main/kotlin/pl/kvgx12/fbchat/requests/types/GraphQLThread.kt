package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.requests.AdditionalInfoResponse
import pl.kvgx12.fbchat.utils.surrogateDeserializer
import pl.kvgx12.fbchat.utils.tryGet

@Serializable
internal data class GraphQLThread(
    @SerialName("thread_key") val threadKey: ThreadKey,
    val name: String? = null,
    @SerialName("messages_count") val messagesCount: Int? = null,
    @SerialName("thread_type") val threadType: String,
    val image: GraphQLImage? = null,
    @SerialName("customization_info") val customization: CustomizationInfo = CustomizationInfo(),
    @SerialName("last_message") val lastMessage: Nodes<LastMessage>,
    val city: Name? = null,
    @SerialName("category_type") val category: String? = null,
    @SerialName("thread_admins") val admins: List<@Serializable(AdminTransformer::class) String> = emptyList(),
    @SerialName("approval_mode") val approvalMode: Int? = null,
    @SerialName("joinable_link") val joinableLink: String? = null,
    @SerialName("all_participants") val participants: Nodes<@Serializable(ParticipantTransformer::class) UserId> = Nodes(),
    @SerialName("group_approval_queue") val approvalQueue: Nodes<@Serializable(ApprovalTransformer::class) String> = Nodes(),
) {
    object AdminTransformer : JsonTransformingSerializer<String>(String.serializer()) {
        override fun transformDeserialize(element: JsonElement) =
            element.tryGet("id") ?: throw SerializationException("Invalid admin: $element")
    }

    object ParticipantTransformer : JsonTransformingSerializer<UserId>(Id.userIdSerializer) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            element.tryGet("messaging_actor") ?: throw SerializationException("Invalid participant: $element")
    }

    object ApprovalTransformer : JsonTransformingSerializer<String>(String.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            element.tryGet("requester").tryGet("id") ?: throw SerializationException("Missing approval key: $element")
    }

    @Serializable
    data class Id(val id: String) {
        companion object {
            val userIdSerializer = surrogateDeserializer<Id, _> { UserId(it.id) }
        }
    }

    @Serializable
    data class ThreadKey(
        @SerialName("thread_fbid") val threadId: String? = null,
        @SerialName("other_user_id") val otherUserId: String? = null,
    ) {
        inline val id: String? get() = threadId ?: otherUserId
    }

    @Serializable
    data class Name(val name: String)

    @Serializable
    data class Nodes<T>(
        val nodes: List<T> = emptyList()
    )

    @Serializable
    data class LastMessage(
        @SerialName("timestamp_precise") val timestamp: String
    )

    @Serializable
    data class CustomizationInfo(
        val emoji: String? = null,
        @SerialName("participant_customizations") val participantCustomizations: List<ParticipantCustomization> = emptyList(),
        @SerialName("outgoing_bubble_color") val color: String = DEFAULT_COLOR,
    )

    @Serializable
    data class ParticipantCustomization(
        @SerialName("participant_id") val participantId: String,
        val nickname: String,
    )

    companion object {
        const val SINGLE = "ONE_TO_ONE"
        const val GROUP = "GROUP"
    }

    fun toThread(sessionUserId: String, profile: AdditionalInfoResponse.Profile?): ThreadData {
        val participantNicknames = customization.participantCustomizations.associate { it.participantId to it.nickname }
        val id = threadKey.id!!
        val lastActivity = lastMessage.nodes.firstOrNull()?.timestamp?.toLongOrNull()

        return when {
            threadType == GROUP -> {
                GroupData(
                    id = id,
                    photo = image,
                    name = name ?: "",
                    lastActive = lastActivity,
                    messageCount = messagesCount,
                    participants = participants.nodes,
                    nicknames = participantNicknames,
                    color = customization.color,
                    emoji = customization.emoji,
                    admins = admins,
                    approvalMode = approvalMode?.let { it != 0 },
                    approvalRequests = approvalQueue.nodes,
                    joinLink = joinableLink,
                )
            }

            threadType == SINGLE && profile?.firstName != null -> {
                UserData(
                    id = id,
                    photo = Image(profile.thumbSrc),
                    name = profile.name,
                    isFriend = profile.isFriend == true,
                    firstName = profile.firstName,
                    lastName = profile.name.removePrefix(profile.firstName).trim(),
                    lastActive = lastActivity,
                    messageCount = messagesCount,
                    url = profile.uri,
                    gender = genderMapping[profile.gender],
                    affinity = null,
                    nickname = participantNicknames[id],
                    ownNickname = participantNicknames[sessionUserId],
                    color = customization.color,
                    emoji = customization.emoji,
                )
            }

            threadType == SINGLE -> {
                require(profile != null) {
                    "Page profile data is missing"
                }

                PageData(
                    id = id,
                    photo = Image(profile.thumbSrc),
                    name = profile.name,
                    lastActive = lastActivity,
                    messageCount = messagesCount,
                    url = profile.uri,
                    city = city?.name,
                    likes = null,
                    subtitle = null,
                    category = category,
                )
            }

            else -> error("Unknown thread type $threadType")
        }
    }
}
