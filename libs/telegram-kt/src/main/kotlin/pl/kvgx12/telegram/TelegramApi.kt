package pl.kvgx12.telegram

import io.ktor.resources.*
import kotlinx.serialization.SerialName

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#getfile)
 *
 * Parameters: Arguments
 *
 * Returns: file
 */
@Resource("/file/bot{botToken}")
class TelegramFile(val botToken: String) {
    @Resource("{path}")
    class Path(val parent: TelegramFile, val path: String)
}


/**
 * [Telegram API Docs](https://core.telegram.org/bots/api)
 */
@Resource("/bot{botToken}/")
class TelegramApi(val botToken: String) {
    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#sendmessage)
     *
     * Parameters: json
     *
     * Returns: [TODO]
     */
    @Resource("sendMessage")
    class SendMessage(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#sendphoto)
     *
     * Parameters: json
     *
     * Returns: [TODO]
     */
    @Resource("sendPhoto")
    class SendPhoto(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#sendaudio)
     *
     * Parameters: json
     *
     * Returns: [TODO]
     */
    @Resource("sendAudio")
    class SendAudio(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#sendmediagroup)
     *
     * Parameters: json
     *
     * Returns: [TODO]
     */
    @Resource("sendMediaGroup")
    class SendMediaGroup(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#getfile)
     *
     * Parameters: Arguments
     *
     * Returns: [TODO]
     */
    @Resource("getFile")
    class GetFile(val parent: TelegramApi, @SerialName("file_id") val fileId: String)


    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#setmessagereaction)
     *
     * Parameters: json
     *
     * Returns: true on success
     */
    @Resource("setMessageReaction")
    class SetMessageReaction(val parent: TelegramApi)


    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#getuserprofilephotos)
     *
     * Parameters: Arguments
     *
     * Returns: [TODO]
     */
    @Resource("getUserProfilePhotos")
    class GetUserProfilePhotos(
        val parent: TelegramApi,
        @SerialName("user_id")
        val userId: Long,
        val offset: Int? = null,
        val limit: Int? = null,
    )

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#getchat)
     *
     * Parameters: Arguments
     *
     * Returns: [TODO]
     */
    @Resource("getChat")
    class GetChat(
        val parent: TelegramApi,
        @SerialName("chat_id")
        val chatId: String,
    )

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#getchatmember)
     *
     * Parameters: Arguments
     *
     * Returns: [TODO]
     */
    @Resource("getChatMember")
    class GetChatMember(
        val parent: TelegramApi,
        @SerialName("user_id")
        val userId: Long,
        @SerialName("chat_id")
        val chatId: String,
    )


    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#getupdates)
     *
     * Parameters: Arguments
     *
     * Returns: [TODO]
     */
    @Resource("getUpdates")
    class GetUpdates(
        val parent: TelegramApi,
        val offset: Long? = null,
        val limit: Int? = null,
        val timeout: Int? = null,
        @SerialName("allowed_updates")
        val allowedUpdates: List<String> = emptyList(),
    )

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#setwebhook)
     *
     * Parameters: json
     *
     * Returns: true on success
     */
    @Resource("setWebhook")
    class SetWebhook(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#deletewebhook)
     *
     * Parameters: Arguments
     *
     * Returns: true on success
     */
    @Resource("deleteWebhook")
    class DeleteWebhook(
        val parent: TelegramApi,
        @SerialName("drop_pending_updates")
        val dropPendingUpdates: Boolean? = null,
    )

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#getwebhookinfo)
     *
     * Parameters: json
     *
     * Returns: [TODO]
     */
    @Resource("getWebhookInfo")
    class GetWebhookInfo(val parent: TelegramApi)


    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#getme)
     *
     * Parameters: No parameters
     *
     * Returns: [pl.kvgx12.telegram.data.User]
     **/
    @Resource("getMe")
    class GetMe(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#logout)
     *
     * Parameters: No parameters
     *
     * Returns: returns true on success
     */
    @Resource("logOut")
    class LogOut(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#close)
     *
     * Parameters: No parameters
     *
     * Returns: true on success
     */
    @Resource("close")
    class Close(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#getmycommands)
     *
     * Parameters: json
     *
     * Returns: [TODO]
     */
    @Resource("getMyCommands")
    class GetMyCommands(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#setmycommands)
     *
     * Parameters: json
     *
     * Returns: true on success
     */
    @Resource("setMyCommands")
    class SetMyCommands(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#deletemycommands)
     *
     * Parameters: json
     *
     * Returns: true on success
     */
    @Resource("deleteMyCommands")
    class DeleteMyCommands(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#setmydefaultadministratorrights)
     *
     * Parameters: json
     *
     * Returns: true on success
     */
    @Resource("setMyDefaultAdministratorRights")
    class SetMyDefaultAdministratorRights(val parent: TelegramApi)

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#setmyname)
     *
     * Parameters: Arguments
     *
     * Returns: true on success
     */
    @Resource("setMyName")
    class SetMyName(
        val parent: TelegramApi,
        val name: String? = null,
        @SerialName("language_code")
        val languageCode: String? = null,
    )

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#setmydescription)
     *
     * Parameters: Arguments
     *
     * Returns: true on success
     */
    @Resource("setMyDescription")
    class SetMyDescription(
        val parent: TelegramApi,
        val description: String? = null,
        @SerialName("language_code")
        val languageCode: String? = null,
    )

    /**
     * [Telegram API Docs](https://core.telegram.org/bots/api#setmyshortdescription)
     *
     * Parameters: Arguments
     *
     * Returns: true on success
     */
    @Resource("setMyShortDescription")
    class SetMyShortDescription(
        val parent: TelegramApi,
        @SerialName("short_description")
        val shortDescription: String? = null,
        @SerialName("language_code")
        val languageCode: String? = null,
    )
}
