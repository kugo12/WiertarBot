package pl.kvgx12.telegram.data


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.NestedJsonSerializer

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#botcommand)
 */
@Serializable
data class TBotCommand(
    val command: String,
    val description: String
)

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#botcommandscope)
 */
@Serializable
sealed interface TBotCommandScope {
    companion object {
        internal object Serializer : NestedJsonSerializer<TBotCommandScope>(serializer())
    }

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#botcommandscopedefault)
     */
    @Serializable
    @SerialName("default")
    data object Default : TBotCommandScope

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#botcommandscopedeallprivatechats)
     */
    @Serializable
    @SerialName("all_private_chats")
    data object AllPrivateChats : TBotCommandScope

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#botcommandscopeallgroupchats)
     */
    @Serializable
    @SerialName("all_group_chats")
    data object AllGroupChats : TBotCommandScope

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#botcommandscopeallchatadministrators)
     */
    @Serializable
    @SerialName("all_chat_administrators")
    data object AllChatAdministrators : TBotCommandScope

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#botcommandscopechat)
     */
    @Serializable
    @SerialName("chat")
    data class Chat(
        @SerialName("chat_id")
        val chatId: String,
    ) : TBotCommandScope

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#botcommandscopechatadministrators)
     */
    @Serializable
    @SerialName("chat_administrators")
    data class ChatAdministrators(
        @SerialName("chat_id")
        val chatId: String,
    ) : TBotCommandScope

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#botcommandscopechatmember)
     */
    @Serializable
    @SerialName("chat_member")
    data class ChatMember(
        @SerialName("chat_id")
        val chatId: String,
        @SerialName("user_id")
        val userId: Long,
    ) : TBotCommandScope
}
