package pl.kvgx12.telegram

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.URLProtocol.Companion.HTTPS
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import pl.kvgx12.telegram.TelegramApi.*
import pl.kvgx12.telegram.data.*
import pl.kvgx12.telegram.data.requests.*

class TelegramClient(
    token: String,
) {
    val basePath = TelegramApi(token)
    val filePath = TelegramFile(token)
    private val client = createHttpClient()

    suspend fun sendMessage(
        request: TSendMessageRequest
    ): TMessage = client.post(SendMessage(basePath)) {
        setBody(request)
    }.body<TMessage>()

    suspend fun sendPhoto(
        request: TSendPhotoRequest
    ): TMessage = client.post(SendPhoto(basePath)) {
        setBody(request)
    }.body<TMessage>()

    suspend fun sendAudio(
        request: TSendAudioRequest
    ): TMessage = client.post(SendAudio(basePath)) {
        setBody(request)
    }.body<TMessage>()

    suspend fun sendMediaGroup(
        request: TSendAudioRequest
    ): List<TMessage> = client.post(SendMediaGroup(basePath)) {
        setBody(request)
    }.body<List<TMessage>>()

    suspend fun getFile(fileId: String): TFile =
        client.get(GetFile(basePath, fileId))
            .body<TFile>()

    suspend fun setMessageReaction(
        messageId: Long,
        chatId: String,
        emoji: String,
    ): Boolean = client.post(SetMessageReaction(basePath)) {
        setBody(TSetMessageReactionRequest(messageId = messageId, chatId = chatId, reaction = listOf(TReactionType.Emoji(emoji))))
    }.body<Boolean>()

    suspend fun getUserProfilePhotos(userId: Long, offset: Int? = null, limit: Int? = null) =
        client.get(GetUserProfilePhotos(basePath, userId, offset, limit))
            .body<TUserProfilePhotos>()

    suspend fun getChat(chatId: String): TChatFullInfo =
        client.get(GetChat(basePath, chatId))
            .body<TChatFullInfo>()

    suspend fun getChatMember(chatId: String, userId: Long): TChatMember =
        client.get(GetChatMember(basePath, userId, chatId))
            .body<TChatMember>()


    suspend fun setWebhook(
        url: String,
        dropPendingUpdates: Boolean = false,
        secretToken: String? = null,
    ): Boolean = client.post(SetWebhook(basePath)) {
        setBody(TSetWebhookRequest(url = url, dropPendingUpdates = dropPendingUpdates, secretToken = secretToken))
    }.body<Boolean>()

    suspend fun deleteWebhook(dropPendingUpdates: Boolean = false): Boolean =
        client.get(DeleteWebhook(basePath))
            .body<Boolean>()

    suspend fun getWebhookInfo(): TWebhookInfo =
        client.get(GetWebhookInfo(basePath))
            .body<TWebhookInfo>()

    suspend fun getMe(): TUser =
        client.get(GetMe(basePath))
            .body<TUser>()

    suspend fun logOut(): Boolean =
        client.get(LogOut(basePath))
            .body<Boolean>()

    suspend fun close(): Boolean =
        client.get(Close(basePath))
            .body<Boolean>()

    suspend fun getMyCommands(): List<TBotCommand> =
        client.get(GetMyCommands(basePath))
            .body<List<TBotCommand>>()

    suspend fun setMyCommands(commands: List<TBotCommand>, scope: TBotCommandScope? = null): Boolean =
        client.post(SetMyCommands(basePath)) {
            setBody(TSetMyCommandsRequest(commands = commands, scope = scope))
        }.body<Boolean>()

    suspend fun deleteMyCommands(scope: TBotCommandScope? = null): Boolean =
        client.post(DeleteMyCommands(basePath)) {
            setBody(TDeleteMyCommandsRequest(scope = scope))
        }.body<Boolean>()

    suspend fun setMyDefaultAdministratorRights(
        rights: TChatAdministratorRights,
    ): Boolean = client.post(SetMyDefaultAdministratorRights(basePath)) {
        setBody(TSetMyDefaultAdministratorRightsRequest(rights = rights))
    }.body<Boolean>()

    suspend fun setMyName(name: String? = null): Boolean =
        client.get(SetMyName(basePath, name = name))
            .body<Boolean>()

    suspend fun setMyDescription(description: String? = null): Boolean =
        client.get(SetMyDescription(basePath, description = description))
            .body<Boolean>()

    suspend fun setMyShortDescription(shortDescription: String? = null): Boolean =
        client.get(SetMyShortDescription(basePath, shortDescription = shortDescription))
            .body<Boolean>()

    companion object {
        private fun createHttpClient() = HttpClient(CIO) {
            install(Resources)

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            ContentEncoding {
                gzip()
                deflate()
            }


            Logging {
                level = LogLevel.ALL
            }

            defaultRequest {
                host = "api.telegram.org"
                url { protocol = HTTPS }
            }
        }
    }
}
