package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.requests.AdditionalInfoResponse
import pl.kvgx12.fbchat.utils.tryGet

@Serializable
internal data class GraphQLThread(
    @SerialName("thread_key")
    val id: @Serializable(ThreadKeyTransformer::class) String,
    val name: String? = null,
    @SerialName("messages_count")
    val messagesCount: Int? = null,
    @SerialName("thread_type")
    val threadType: String,
    val image: GraphQLImage? = null,
    @SerialName("customization_info")
    val customization: CustomizationInfo = CustomizationInfo(),
    @SerialName("last_message")
    val lastMessage: @Serializable(NodesTransformer::class) List<LastMessage>,
    val city: @Serializable(NameTransformer::class) String? = null,
    @SerialName("category_type") val category: String? = null,
    @SerialName("thread_admins")
    val admins: List<
        @Serializable(AdminTransformer::class)
        String,
        > = emptyList(),
    @SerialName("approval_mode")
    val approvalMode: Int? = null,
    @SerialName("joinable_link")
    val joinableLink: String? = null,
    @SerialName("all_participants")
    val participants: @Serializable(EdgesTransformer::class) List<
        @Serializable(ParticipantTransformer::class)
        Participant,
        > = listOf(),
    @SerialName("group_approval_queue")
    val approvalQueue: @Serializable(NodesTransformer::class) List<
        @Serializable(ApprovalTransformer::class)
        String,
        > = listOf(),
) {
    object AdminTransformer : JsonTransformingSerializer<String>(String.serializer()) {
        override fun transformDeserialize(element: JsonElement) =
            element.tryGet("id") ?: throw SerializationException("Invalid admin: $element")
    }

    object ParticipantTransformer : JsonTransformingSerializer<Participant>(Participant.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            element.tryGet("node").tryGet("messaging_actor") ?: throw SerializationException("Invalid participant: $element")
    }

    object ApprovalTransformer : JsonTransformingSerializer<String>(String.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            element.tryGet("requester").tryGet("id") ?: throw SerializationException("Missing approval key: $element")
    }

    object ParticipantImageTransformer : JsonTransformingSerializer<String>(String.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            element.tryGet("uri") ?: throw SerializationException("Invalid participant image: $element")
    }

    class EdgesTransformer<T>(serializer: KSerializer<T>) : JsonTransformingSerializer<List<T>>(ListSerializer(serializer)) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            element.tryGet("edges") ?: throw SerializationException("Invalid edges: $element")
    }

    class NodesTransformer<T>(serializer: KSerializer<T>) : JsonTransformingSerializer<List<T>>(ListSerializer(serializer)) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            element.tryGet("nodes") ?: throw SerializationException("Invalid nodes: $element")
    }

    object ThreadKeyTransformer : JsonTransformingSerializer<String>(String.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            element.tryGet("thread_fbid")
                ?: element.tryGet("other_user_id")
                ?: throw SerializationException("Invalid thread key: $element")
    }

    object NameTransformer : JsonTransformingSerializer<String>(String.serializer()) {
        override fun transformDeserialize(element: JsonElement): JsonElement =
            element.tryGet("name") ?: throw SerializationException("Invalid name: $element")
    }

    @Serializable
    data class Participant(
        val id: String,
        val name: String = "",
        val gender: String = "",
        @SerialName("big_image_src")
        val imageUri: @Serializable(ParticipantImageTransformer::class) String = "",
        @SerialName("short_name")
        val shortName: String = "",
        val username: String = ""
    )

    @Serializable
    data class LastMessage(
        @SerialName("timestamp_precise") val timestamp: String,
    )

    @Serializable
    data class CustomizationInfo(
        val emoji: String? = null,
        @SerialName("participant_customizations")
        val participantCustomizations: List<ParticipantCustomization> = emptyList(),
        @SerialName("outgoing_bubble_color")
        val color: String = DEFAULT_COLOR,
    )

    @Serializable
    data class ParticipantCustomization(
        @SerialName("participant_id")
        val participantId: String,
        val nickname: String,
    )

    companion object {
        const val SINGLE = "ONE_TO_ONE"
        const val GROUP = "GROUP"
    }

    @Suppress("LongMethod")
    fun toThread(sessionUserId: String, profile: AdditionalInfoResponse.Profile?): ThreadData {
        val participantNicknames = customization.participantCustomizations.associate { it.participantId to it.nickname }
        val lastActivity = lastMessage.firstOrNull()?.timestamp?.toLongOrNull()
        val participants = participants.map {
            ThreadParticipant(
                id = it.id,
                name = it.name,
                gender = it.gender,
                imageUri = it.imageUri,
                shortName = it.shortName,
                username = it.username,
            )
        }

        return when (threadType) {
            GROUP -> {
                GroupData(
                    id = id,
                    photo = image,
                    name = name.orEmpty(),
                    lastActive = lastActivity,
                    messageCount = messagesCount,
                    participants = participants,
                    nicknames = participantNicknames,
                    color = customization.color,
                    emoji = customization.emoji,
                    admins = admins,
                    approvalMode = approvalMode?.let { it != 0 },
                    approvalRequests = approvalQueue,
                    joinLink = joinableLink,
                )
            }

            SINGLE if profile?.firstName != null -> {
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
                    nickname = participantNicknames[this.id],
                    ownNickname = participantNicknames[sessionUserId],
                    color = customization.color,
                    emoji = customization.emoji,
                    participants = participants
                )
            }

            SINGLE -> {
                requireNotNull(profile) {
                    "Page profile data is missing"
                }

                PageData(
                    id = id,
                    photo = Image(profile.thumbSrc),
                    name = profile.name,
                    lastActive = lastActivity,
                    messageCount = messagesCount,
                    url = profile.uri,
                    city = city,
                    likes = null,
                    subtitle = null,
                    category = category,
                    participants = participants
                )
            }

            else -> error("Unknown thread type $threadType")
        }
    }
}
