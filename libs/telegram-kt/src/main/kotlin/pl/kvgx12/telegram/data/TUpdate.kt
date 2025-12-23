package pl.kvgx12.telegram.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TUpdate(
    @SerialName("update_id")
    val updateId: Long,

    val message: TMessage? = null,
    val editedMessage: TMessage? = null,
    val channelPost: TMessage? = null,
    val editedChannelPost: TMessage? = null,
    val businessMessage: TMessage? = null,
    val editedBusinessMessage: TMessage? = null,

    @SerialName("my_chat_member")
    val myChatMember: TChatMemberUpdate? = null,
    @SerialName("chat_member")
    val chatMember: TChatMemberUpdate? = null,
)


@Serializable
data class TChatMemberUpdate(
    val chat: TChat,
    val from: TUser,
    val date: Long,
    @SerialName("old_chat_member")
    val oldChatMember: TChatMember,
    @SerialName("new_chat_member")
    val newChatMember: TChatMember,
    @SerialName("invite_link")
    val inviteLink: TChatInviteLink? = null,
    @SerialName("via_join_request")
    val viaJoinRequest: Boolean? = null,
    @SerialName("via_chat_folder_invite_link")
    val viaChatFolderInviteLink: Boolean? = null,
)

@Serializable
data class TChatInviteLink(
    @SerialName("invite_link")
    val inviteLink: String,
)
