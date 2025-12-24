@file:Suppress("TooManyFunctions")

package pl.kvgx12.telegram

import io.ktor.client.plugins.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import pl.kvgx12.telegram.TelegramApi.*
import pl.kvgx12.telegram.data.*
import pl.kvgx12.telegram.data.requests.*

class TelegramClient(
    token: String,
) {
    private val log = LoggerFactory.getLogger(TelegramClient::class.java)
    val basePath = TelegramApi("bot$token")
    val filePath = TelegramFile("bot$token")
    val client = createHttpClient()

    suspend fun sendTextMessage(
        chatId: String,
        text: String,
        entities: List<TMessageEntity> = emptyList(),
        replyParameters: TReplyParameters? = null,
    ): TMessage = client.post<SendMessage>(SendMessage(basePath)) {
        setBody(
            TSendMessageRequest(
                chatId = chatId,
                text = text,
                entities = entities,
                replyParameters = replyParameters
            )
        )
    }.tResult()

    suspend fun sendPhoto(
        chatId: String,
        photo: TInputFile,
        caption: String? = null,
        captionEntities: List<TMessageEntity> = emptyList(),
        replyParameters: TReplyParameters? = null,
    ): TMessage {
        val request = TSendPhotoRequest(
            chatId = chatId,
            photo = photo as? TInputFile.UrlOrId,
            caption = caption,
            captionEntities = captionEntities,
            replyParameters = replyParameters
        )

        return client.post<SendPhoto>(SendPhoto(basePath)) {
            when (photo) {
                is TInputFile.UrlOrId -> setBody(request)
                is TInputFile.Upload -> multipartForm(
                    TSendPhotoRequest.serializer(),
                    request,
                    mapOf("photo" to photo)
                )
            }
        }.tResult()
    }

    suspend fun sendAudio(
        chatId: String,
        audio: TInputFile,
        caption: String? = null,
        captionEntities: List<TMessageEntity> = emptyList(),
        replyParameters: TReplyParameters? = null,
    ): TMessage {
        val request = TSendAudioRequest(
            chatId = chatId,
            audio = audio as? TInputFile.UrlOrId,
            caption = caption,
            captionEntities = captionEntities,
            replyParameters = replyParameters
        )

        return client.post(SendAudio(basePath)) {
            when (audio) {
                is TInputFile.UrlOrId -> setBody(request)
                is TInputFile.Upload -> multipartForm(
                    TSendAudioRequest.serializer(),
                    request,
                    mapOf("audio" to audio)
                )
            }
        }.tResult()
    }

    suspend fun <T : TInputMedia> sendMediaGroup(
        chatId: String,
        media: List<T>,
        replyParameters: TReplyParameters? = null,
    ): List<TMessage> {
        val mp = mutableListOf<TInputFile.Upload>()
        val mediaWithPlaceholders = media.mapIndexed { index, inputMedia ->
            if (inputMedia.media is TInputFile.Upload) {
                val placeholder = "attach://file$index"
                mp.add(inputMedia.media as TInputFile.Upload)

                when (inputMedia) {
                    is TInputMedia.Photo -> inputMedia.copy(media = TInputFile.UrlOrId(placeholder))
                    is TInputMedia.Video -> inputMedia.copy(media = TInputFile.UrlOrId(placeholder))
                    is TInputMedia.Audio -> inputMedia.copy(media = TInputFile.UrlOrId(placeholder))
                    is TInputMedia.Document -> inputMedia.copy(media = TInputFile.UrlOrId(placeholder))
                }
            } else {
                inputMedia
            }
        }
        val request = TSendMediaGroupRequest(
            chatId = chatId,
            media = mediaWithPlaceholders,
            replyParameters = replyParameters
        )

        return client.post(SendMediaGroup(basePath)) {
            multipartForm(
                TSendMediaGroupRequest.serializer(TInputMedia.serializer()),
                request,
                mp.mapIndexed { index, upload -> "file$index" to upload }.toMap()
            )
        }.tResult()
    }


    suspend fun getFile(fileId: String): TFile =
        client.get(GetFile(basePath, fileId))
            .tResult()

    suspend fun setMessageReaction(
        messageId: Long,
        chatId: String,
        emoji: String,
    ): Boolean = client.post(SetMessageReaction(basePath)) {
        setBody(TSetMessageReactionRequest(messageId = messageId, chatId = chatId, reaction = listOf(TReactionType.Emoji(emoji))))
    }.tResult()

    suspend fun getUserProfilePhotos(userId: Long, offset: Int? = null, limit: Int? = null): TUserProfilePhotos =
        client.get(GetUserProfilePhotos(basePath, userId, offset, limit))
            .tResult()

    suspend fun getChat(chatId: String): TChatFullInfo =
        client.get(GetChat(basePath, chatId))
            .tResult()

    suspend fun getChatMember(chatId: String, userId: Long): TChatMember =
        client.get(GetChatMember(basePath, userId, chatId))
            .tResult()


    suspend fun setWebhook(
        url: String,
        dropPendingUpdates: Boolean = false,
        secretToken: String? = null,
    ): Boolean = client.post(SetWebhook(basePath)) {
        setBody(TSetWebhookRequest(url = url, dropPendingUpdates = dropPendingUpdates, secretToken = secretToken))
    }.tResult()

    suspend fun deleteWebhook(dropPendingUpdates: Boolean = false): Boolean =
        client.get(DeleteWebhook(basePath, dropPendingUpdates = dropPendingUpdates))
            .tResult()

    suspend fun getWebhookInfo(): TWebhookInfo =
        client.get(GetWebhookInfo(basePath))
            .tResult()

    suspend fun getMe(): TUser =
        client.get(GetMe(basePath))
            .tResult()

    suspend fun logOut(): Boolean =
        client.get(LogOut(basePath))
            .tResult()

    suspend fun close(): Boolean =
        client.get(Close(basePath))
            .tResult()

    suspend fun getMyCommands(): List<TBotCommand> =
        client.get(GetMyCommands(basePath))
            .tResult()

    suspend fun setMyCommands(commands: List<TBotCommand>, scope: TBotCommandScope? = null): Boolean =
        client.post(SetMyCommands(basePath)) {
            setBody(TSetMyCommandsRequest(commands = commands, scope = scope))
        }.tResult()

    suspend fun deleteMyCommands(scope: TBotCommandScope? = null): Boolean =
        client.post(DeleteMyCommands(basePath)) {
            setBody(TDeleteMyCommandsRequest(scope = scope))
        }.tResult()

    suspend fun setMyDefaultAdministratorRights(
        rights: TChatAdministratorRights,
    ): Boolean = client.post(SetMyDefaultAdministratorRights(basePath)) {
        setBody(TSetMyDefaultAdministratorRightsRequest(rights = rights))
    }.tResult()

    suspend fun setMyName(name: String? = null): Boolean =
        client.get(SetMyName(basePath, name = name))
            .tResult()

    suspend fun setMyDescription(description: String? = null): Boolean =
        client.get(SetMyDescription(basePath, description = description))
            .tResult()

    suspend fun setMyShortDescription(shortDescription: String? = null): Boolean =
        client.get(SetMyShortDescription(basePath, shortDescription = shortDescription))
            .tResult()

    @Suppress("TooGenericExceptionCaught")
    fun getUpdates(
        offset: Long? = null,
        limit: Int? = null,
        timeout: Int? = 30,
        allowedUpdates: List<String> = emptyList()
    ): Flow<Update> = flow {
        var currentOffset = offset ?: 0

        deleteWebhook()

        while (true) {
            try {
                val updates = client.get(GetUpdates(basePath, currentOffset, limit, timeout, allowedUpdates)) {
                    timeout {
                        requestTimeoutMillis = (timeout ?: 0) * 1000L + 10000L
                    }
                }.tResult<List<TUpdate>>()

                log.debug("Received {}", updates)

                for (tUpdate in updates) {
                    val update = tUpdate.toUpdate()

                    if (update != null)
                        emit(update)

                    currentOffset = tUpdate.updateId + 1
                }
            } catch (e: Exception) {
                log.error("Error while getting updates", e)
                delay(1000)
            }
        }
    }
}
