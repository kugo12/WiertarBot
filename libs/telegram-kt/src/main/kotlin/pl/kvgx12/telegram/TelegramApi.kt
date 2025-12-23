package pl.kvgx12.telegram

import io.ktor.resources.*
import kotlinx.serialization.SerialName

// https://core.telegram.org/bots/api#getfile
// params
@Resource("/file/bot{botToken}")
class TelegramFile(val botToken: String) {
    @Resource("{path}")
    class Path(val parent: TelegramFile, val path: String)
}


// https://core.telegram.org/bots/api
@Resource("/bot{botToken}/")
class TelegramApi(val botToken: String) {
    // https://core.telegram.org/bots/api#sendmessage
    // json
    @Resource("sendMessage")
    class SendMessage(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#sendphoto
    // json
    @Resource("sendPhoto")
    class SendPhoto(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#sendaudio
    // json
    @Resource("sendAudio")
    class SendAudio(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#sendmediagroup
    // json
    @Resource("sendMediaGroup")
    class SendMediaGroup(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#getfile
    // params
    @Resource("getFile")
    class GetFile(val parent: TelegramApi, @SerialName("file_id") val fileId: String)


    // https://core.telegram.org/bots/api#setmessagereaction
    // json
    @Resource("setMessageReaction")
    class SetMessageReaction(val parent: TelegramApi)


    // https://core.telegram.org/bots/api#getuserprofilephotos
    // params
    @Resource("getUserProfilePhotos")
    class GetUserProfilePhotos(
        val parent: TelegramApi,
        @SerialName("user_id")
        val userId: Long,
        val offset: Int? = null,
        val limit: Int? = null,
    )

    // https://core.telegram.org/bots/api#getchat
    // params
    @Resource("getChat")
    class GetChat(
        val parent: TelegramApi,
        @SerialName("chat_id")
        val chatId: String,
    )

    // https://core.telegram.org/bots/api#getchatmember
    // params
    @Resource("getChatMember")
    class GetChatMember(
        val parent: TelegramApi,
        @SerialName("user_id")
        val userId: Long,
        @SerialName("chat_id")
        val chatId: String,
    )


    // https://core.telegram.org/bots/api#getupdates
    // params
    @Resource("getUpdates")
    class GetUpdates(
        val parent: TelegramApi,
        val offset: Long? = null,
        val limit: Int? = null,
        val timeout: Int? = null,
        @SerialName("allowed_updates")
        val allowedUpdates: List<String> = emptyList(),
    )

    // https://core.telegram.org/bots/api#setwebhook
    // json
    @Resource("setWebhook")
    class SetWebhook(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#deletewebhook
    // params
    @Resource("deleteWebhook")
    class DeleteWebhook(
        val parent: TelegramApi,
        @SerialName("drop_pending_updates")
        val dropPendingUpdates: Boolean? = null,
    )

    // https://core.telegram.org/bots/api#getwebhookinfo
    // json
    @Resource("getWebhookInfo")
    class GetWebhookInfo(val parent: TelegramApi)


    // https://core.telegram.org/bots/api#getme
    // no args
    @Resource("getMe")
    class GetMe(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#logout
    // no args
    @Resource("logOut")
    class LogOut(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#close
    // no args
    @Resource("close")
    class Close(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#getmycommands
    // json
    @Resource("getMyCommands")
    class GetMyCommands(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#setmycommands
    // json
    @Resource("setMyCommands")
    class SetMyCommands(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#deletemycommands
    // json
    @Resource("deleteMyCommands")
    class DeleteMyCommands(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#setmydefaultadministratorrights
    // json
    @Resource("setMyDefaultAdministratorRights")
    class SetMyDefaultAdministratorRights(val parent: TelegramApi)

    // https://core.telegram.org/bots/api#setmyname
    // params
    @Resource("setMyName")
    class SetMyName(
        val parent: TelegramApi,
        val name: String? = null,
        @SerialName("language_code")
        val languageCode: String? = null,
    )

    // https://core.telegram.org/bots/api#setmydescription
    // params
    @Resource("setMyDescription")
    class SetMyDescription(
        val parent: TelegramApi,
        val description: String? = null,
        @SerialName("language_code")
        val languageCode: String? = null,
    )

    // https://core.telegram.org/bots/api#setmyshortdescription
    // params
    @Resource("setMyShortDescription")
    class SetMyShortDescription(
        val parent: TelegramApi,
        @SerialName("short_description")
        val shortDescription: String? = null,
        @SerialName("language_code")
        val languageCode: String? = null,
    )
}
