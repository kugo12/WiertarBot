package pl.kvgx12.telegram.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import pl.kvgx12.telegram.NestedJsonSerializer

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#chat)
 */
@Serializable
data class TChat(
    val id: Long,
    val type: String,
    val title: String? = null,
    val username: String? = null,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
)

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#chatadministratorrights)
 */
@Serializable
data class TChatAdministratorRights(
    @SerialName("is_anonymous")
    val isAnonymous: Boolean,
    @SerialName("can_manage_chat")
    val canManageChat: Boolean,
    @SerialName("can_delete_messages")
    val canDeleteMessages: Boolean,
    @SerialName("can_manage_video_chats")
    val canManageVideoChats: Boolean,
    @SerialName("can_restrict_members")
    val canRestrictMembers: Boolean,
    @SerialName("can_promote_members")
    val canPromoteMembers: Boolean,
    @SerialName("can_change_info")
    val canChangeInfo: Boolean,
    @SerialName("can_invite_users")
    val canInviteUsers: Boolean,
    @SerialName("can_post_stories")
    val canPostStories: Boolean,
    @SerialName("can_edit_stories")
    val canEditStories: Boolean,
    @SerialName("can_delete_stories")
    val canDeleteStories: Boolean,
    @SerialName("can_post_messages")
    val canPostMessages: Boolean,
    @SerialName("can_edit_messages")
    val canEditMessages: Boolean,
    @SerialName("can_pin_messages")
    val canPinMessages: Boolean,
    @SerialName("can_manage_topics")
    val canManageTopics: Boolean,
    @SerialName("can_manage_direct_messages")
    val canManageDirectMessages: Boolean,
) {
    internal object Serializer : NestedJsonSerializer<TChatAdministratorRights>(serializer())
}

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#chatmember)
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("status")
sealed interface TChatMember {
    val user: TUser

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#chatmemberowner)
     */
    @Serializable
    @SerialName("creator")
    data class Owner(
        override val user: TUser,
        @SerialName("is_anonymous")
        val isAnonymous: Boolean,
        @SerialName("custom_title")
        val customTitle: String? = null,
    ) : TChatMember

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#chatmemberadministrator)
     */
    @Serializable
    @SerialName("administrator")
    data class Administrator(
        override val user: TUser,
        @SerialName("custom_title")
        val customTitle: String? = null,
        @SerialName("is_anonymous")
        val isAnonymous: Boolean,
    ) : TChatMember

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#chatmembermember)
     */
    @Serializable
    @SerialName("member")
    data class Member(override val user: TUser) : TChatMember

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#chatmemberrestricted)
     */
    @Serializable
    @SerialName("restricted")
    data class Restricted(
        override val user: TUser,
        @SerialName("is_member")
        val isMember: Boolean,
    ) : TChatMember

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#chatmemberleft)
     */
    @Serializable
    @SerialName("left")
    data class Left(override val user: TUser) : TChatMember

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#chatmemberbanned)
     */
    @Serializable
    @SerialName("kicked")
    data class Banned(override val user: TUser) : TChatMember
}

@Serializable
data class TChatFullInfo(
    val id: Long,
    val type: String,
    val title: String? = null,
    val username: String? = null,
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    @SerialName("active_usernames")
    val activeUsernames: List<String> = emptyList(),
    @SerialName("available_reactions")
    val availableReactions: List<TReactionType> = emptyList(),
    val bio: String? = null,
    val description: String? = null,
)
