package pl.kvgx12.telegram.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TUpdate(
    @SerialName("update_id")
    val updateId: Long,

    val message: TMessage? = null,
    @SerialName("edited_message")
    val editedMessage: TMessage? = null,
    @SerialName("channel_post")
    val channelPost: TMessage? = null,
    @SerialName("edited_channel_post")
    val editedChannelPost: TMessage? = null,
    @SerialName("business_message")
    val businessMessage: TMessage? = null,
    @SerialName("edited_business_message")
    val editedBusinessMessage: TMessage? = null,

    @SerialName("my_chat_member")
    val myChatMember: TChatMemberUpdate? = null,
    @SerialName("chat_member")
    val chatMember: TChatMemberUpdate? = null,
) {
    fun toUpdate(): Update? = when {
        message != null -> Update.Message(Update.Message.Type.Normal, message)
        editedMessage != null -> Update.EditedMessage(Update.Message.Type.Normal, editedMessage)
        channelPost != null -> Update.Message(Update.Message.Type.Channel, channelPost)
        editedChannelPost != null -> Update.EditedMessage(Update.Message.Type.Channel, editedChannelPost)
        businessMessage != null -> Update.Message(Update.Message.Type.Business, businessMessage)
        editedBusinessMessage != null -> Update.EditedMessage(Update.Message.Type.Business, editedBusinessMessage)
        myChatMember != null -> Update.MyChatMember(myChatMember)
        chatMember != null -> Update.ChatMember(chatMember)
        else -> null
    }
}


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

sealed interface Update {
    data class Message(val type: Type, val data: TMessage) : Update {
        enum class Type { Normal, Channel, Business }
    }

    data class EditedMessage(val type: Message.Type, val data: TMessage) : Update

    data class ChatMember(val data: TChatMemberUpdate) : Update
    data class MyChatMember(val data: TChatMemberUpdate) : Update
}
