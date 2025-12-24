package pl.kvgx12.wiertarbot.telegram

import com.google.protobuf.kotlin.toByteString
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import pl.kvgx12.telegram.TelegramFile
import pl.kvgx12.telegram.data.TMessage
import pl.kvgx12.telegram.data.TMessageEntity
import pl.kvgx12.telegram.data.TReplyParameters
import pl.kvgx12.telegram.data.requests.TInputFile
import pl.kvgx12.telegram.data.requests.TInputMedia
import pl.kvgx12.telegram.telegramApiUrl
import pl.kvgx12.wiertarbot.connector.ConnectorContextServer
import pl.kvgx12.wiertarbot.connector.DelegatedCommandInvoker
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.proto.connector.*
import pl.kvgx12.wiertarbot.proto.connector.Empty
import kotlin.io.path.Path
import kotlin.io.path.readBytes

class TelegramKtContext(
    private val connector: TelegramKtConnector,
    delegatedCommantInvoker: DelegatedCommandInvoker,
) : ConnectorContextServer(ConnectorType.TELEGRAM, delegatedCommantInvoker) {
    private val client = connector.client

    private suspend fun sendFiles(response: Response, files: List<UploadedFile>): TMessage? = coroutineScope {
        val replyToId = response.replyParameters()

        if (files.size == 1) {
            val uploadedFile = files.first()
            val mimeType = uploadedFile.mimeType
            val entities = response.buildEntities()

            return@coroutineScope when { // TODO: mimetype as enum?
                mimeType.startsWith("image") -> client.sendPhoto(
                    chatId = response.event.threadId,
                    photo = TInputFile.Upload(uploadedFile.content.toByteArray(), uploadedFile.id),
                    replyParameters = replyToId,
                    caption = response.text,
                    captionEntities = entities,
                )

                mimeType.startsWith("audio") -> client.sendAudio(
                    chatId = response.event.threadId,
                    audio = TInputFile.Upload(uploadedFile.content.toByteArray(), uploadedFile.id),
                    replyParameters = replyToId,
                    caption = response.text,
                    captionEntities = entities,
                )

                else -> error("mime type $mimeType not supported")
            }
        } else if (files.size >= 2) {
            return@coroutineScope client.sendMediaGroup(
                chatId = response.event.threadId,
                media = files.mapIndexed { index, it ->
                    val caption = if (index == 0) response.text else null
                    val entities = if (index == 0) response.buildEntities() else emptyList()

                    when {
                        it.mimeType.startsWith("image") -> TInputMedia.Photo(
                            media = TInputFile.Upload(it.content.toByteArray(), it.id),
                            caption = caption,
                            captionEntities = entities,
                        )

                        it.mimeType.startsWith("audio") -> TInputMedia.Audio(
                            media = TInputFile.Upload(it.content.toByteArray(), it.id),
                            caption = caption,
                            captionEntities = entities,
                        )

                        else -> error("mime type ${it.mimeType} not supported")
                    }
                },
                replyParameters = replyToId,
            ).first()
        }

        null
    }

    override suspend fun send(request: Response): SendResponse {
        if (!request.filesList.isNullOrEmpty()) {
            val result = sendFiles(request, request.filesList)
            if (result != null) {
                return sendResponse {
                    messageId = result.messageId.toString()
                }
            }
        }

        if (!request.text.isNullOrEmpty()) {
            val result = client.sendTextMessage(
                chatId = request.event.threadId,
                text = request.text,
                replyParameters = request.replyParameters(),
                entities = request.buildEntities(),
            )

            return sendResponse {
                messageId = result.messageId.toString()
            }
        }

        return sendResponse {
            messageId = "" // TODO
        }
    }

    override suspend fun uploadRaw(request: UploadRawRequest): UploadResponse = uploadResponse {
        files += request.filesList.map {
            uploadedFile {
                id = it.uri
                mimeType = it.mimeType
                content = it.content
            }
        }
    }

    override suspend fun fetchThread(request: FetchThreadRequest): FetchThreadResponse {
        val chat = client.getChat(request.threadId)

        return fetchThreadResponse {
            thread = threadData {
                id = request.threadId
                name = chat.title
                    ?: chat.username
                        ?: chat.firstName
                        ?: ""
                participants += when (chat.type) {
                    "private" -> listOf(
                        threadParticipant {
                            id = ""
                            username = chat.username
                                ?: chat.firstName
                                    ?: ""
                        },
                        threadParticipant {
                            id = connector.me.id.toString()
                            username = connector.me.username!!
                        }
                    )

                    else -> chat.activeUsernames.map {
                        threadParticipant {
                            id = ""
                            username = it
                        }
                    }
                }
            }
        }
    }

    override suspend fun fetchImageUrl(request: FetchImageUrlRequest): FetchImageUrlResponse = fetchImageUrlResponse {
        val file = client.getFile(request.id)
        val builder = URLBuilder(telegramApiUrl)

        client.client.href(
            TelegramFile.Path(client.filePath, file.filePath!!),
            builder
        )
        url = builder.buildString()
    }

    override suspend fun sendText(request: SendTextRequest): Empty {
        client.sendTextMessage(chatId = request.event.threadId, text = request.text)

        return Empty.getDefaultInstance()
    }

    override suspend fun reactToMessage(request: ReactToMessageRequest): Empty {
        client.setMessageReaction(
            messageId = request.event.messageId.toLong(),
            chatId = request.event.threadId,
            emoji = request.reaction,
        )

        return Empty.getDefaultInstance()
    }

    override suspend fun fetchRepliedTo(request: FetchRepliedToRequest): FetchRepliedToResponse = fetchRepliedToResponse {
        request.event.replyTo?.let {
            event = it
        }
    }

    override suspend fun upload(request: UploadRequest): UploadResponse = coroutineScope {
        uploadResponse {
            files += request.filesList.map { async { downloadFile(it) } }
                .awaitAll()
        }
    }

    companion object {
        private fun Response.buildEntities() = mentionsList?.map {
            TMessageEntity(
                type = TMessageEntity.Type.Mention,
                offset = it.offset,
                length = it.length,
            )
        }.orEmpty()

        private fun Response.replyParameters() =
            replyToId?.toLongOrNull()?.let {
                TReplyParameters(
                    messageId = it,
                    chatId = event.threadId,
                    allowSendingWithoutReply = true,
                )
            }

        private suspend fun downloadFile(urlString: String): UploadedFile {
            val url = Url(urlString)
            if (!urlString.startsWith(url.protocol.name)) {
                return withContext(Dispatchers.IO) {
                    val path = Path(urlString)
                    val content = path.readBytes()
                    val contentType = path.contentType()!!

                    uploadedFile {
                        id = urlString
                        mimeType = contentType
                        this.content = content.toByteString()
                    }
                }
            }

            val response = client.get(url)
            val contentType = response.contentTypeOrNull()
                ?.let { "${it.contentType}/${it.contentSubtype}" }
                ?: Path(urlString).contentType()!!

            require(response.status.value in 200..299) {
                "GET $url status ${response.status}"
            }
            val content = response.body<ByteArray>()

            return uploadedFile {
                id = url.fullPath
                mimeType = contentType
                this.content = content.toByteString()
            }
        }

        private val client = HttpClient { }
    }
}
