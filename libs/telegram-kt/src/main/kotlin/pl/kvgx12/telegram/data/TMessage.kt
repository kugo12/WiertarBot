package pl.kvgx12.telegram.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#message)
 */
@Serializable
data class TMessage(
    val messageId: Long,
    val messageThreadId: Long? = null,
    val from: TUser? = null,
    @SerialName("sender_chat")
    val senderChat: TChat? = null,
    val date: Long,
    val chat: TChat,
    @SerialName("reply_to_message")
    val replyToMessage: TMessage? = null,
// TODO: if it matters
//    @SerialName("external_reply")
//    val externalReply: TExternalReply? = null,
    @SerialName("via_bot")
    val viaBot: TUser? = null,
    @SerialName("media_group_id")
    val mediaGroupId: String? = null,
    val text: String? = null,
    val entities: List<TMessageEntity> = emptyList(),
    val photo: List<TPhotoSize> = emptyList(),
    val caption: String? = null,
    @SerialName("caption_entities")
    val captionEntities: List<TMessageEntity> = emptyList(),

    @SerialName("new_chat_members")
    val newChatMembers: List<TUser> = emptyList(),
    @SerialName("left_chat_member")
    val leftChatMember: TUser? = null,
)

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#messageentity)
 */
@Serializable
data class TMessageEntity(
    val type: Type,
    val offset: Int,
    val length: Int,
    val url: String? = null,
    val user: TUser? = null,
    val language: String? = null,
    @SerialName("custom_emoji_id")
    val customEmojiId: String? = null,
) {
    @Serializable
    enum class Type {
        @SerialName("mention")
        Mention,

        @SerialName("hashtag")
        Hashtag,

        @SerialName("cashtag")
        Cashtag,

        @SerialName("bot_command")
        BotCommand,

        @SerialName("url")
        Url,

        @SerialName("email")
        Email,

        @SerialName("phone_number")
        PhoneNumber,

        @SerialName("bold")
        Bold,

        @SerialName("italic")
        Italic,

        @SerialName("underline")
        Underline,

        @SerialName("strikethrough")
        Strikethrough,

        @SerialName("spoiler")
        Spoiler,

        @SerialName("blockquote")
        Blockquote,

        @SerialName("expandable_blockquote")
        ExpandableBlockquote,

        @SerialName("code")
        Code,

        @SerialName("pre")
        Pre,

        @SerialName("text_link")
        TextLink,

        @SerialName("text_mention")
        TextMention,

        @SerialName("custom_emoji")
        CustomEmoji,
    }
}

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#photosize)
 */
@Serializable
data class TPhotoSize(
    val fileId: String,
    val width: Int,
    val height: Int,
    @SerialName("file_size")
    val fileSize: Long? = null,
)

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#replyparameters)
 */
@Serializable
data class TReplyParameters(
    @SerialName("message_id")
    val messageId: Long,
    @SerialName("chat_id")
    val chatId: String? = null,

    @SerialName("allow_sending_without_reply")
    val allowSendingWithoutReply: Boolean = false,
)

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#file)
 */
@Serializable
data class TFile(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("file_path")
    val filePath: String? = null,
)
