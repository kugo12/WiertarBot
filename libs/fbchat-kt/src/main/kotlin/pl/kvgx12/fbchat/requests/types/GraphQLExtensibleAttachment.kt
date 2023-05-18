@file:OptIn(ExperimentalSerializationApi::class)

package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import pl.kvgx12.fbchat.data.*

@Serializable
internal data class GraphQLExtensibleAttachment(
    @SerialName("story_attachment")
    val storyAttachment: StoryAttachment? = null,
    @SerialName("legacy_attachment_id")
    val legacyAttachmentId: String? = null,
) {
    @Serializable
    data class StoryAttachment(
        val target: Target? = null
    )

    @Serializable
    @JsonClassDiscriminator("__typename")
    sealed interface Target {
        @Serializable
        @SerialName("MessageLocation")
        data class MessageLocation(@SerialName("deduplication_key") val id: String) : Target

        @Serializable
        @SerialName("MessageLiveLocation")
        data class MessageLiveLocation(val id: String) : Target

        @Serializable
        sealed class Shared : Target {
            abstract val id: String?
        }

        @Serializable
        @SerialName("ExternalUrl")
        data class ExternalUrl(
            @SerialName("deduplication_key")
            override val id: String? = null,
        ) : Shared()

        @Serializable
        @SerialName("Story")
        data class Story(
            @SerialName("deduplication_key")
            override val id: String? = null,
        ) : Shared()
    }

    fun toAttachment(): Attachment? {
        val story = storyAttachment ?: return null

        return when (val target = story.target) {
            null -> UnsentMessage(legacyAttachmentId)
            is Target.Shared -> ShareAttachment(target.id)
            is Target.MessageLocation -> LocationAttachment(target.id)
            is Target.MessageLiveLocation -> LiveLocationAttachment(target.id)
        }
    }
}
