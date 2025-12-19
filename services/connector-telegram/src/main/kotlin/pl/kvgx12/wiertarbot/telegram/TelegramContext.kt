package pl.kvgx12.wiertarbot.telegram

import com.google.protobuf.kotlin.toByteString
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.chat.get.GetChat
import dev.inmo.tgbotapi.requests.get.GetFile
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.requests.send.SetMessageReactions
import dev.inmo.tgbotapi.requests.send.media.*
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.chat.ExtendedChatWithUsername
import dev.inmo.tgbotapi.types.chat.PublicChat
import dev.inmo.tgbotapi.types.chat.UsernameChat
import dev.inmo.tgbotapi.types.files.fullUrl
import dev.inmo.tgbotapi.types.media.*
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent
import dev.inmo.tgbotapi.types.message.textsources.mentionTextSource
import dev.inmo.tgbotapi.types.message.textsources.regularTextSource
import dev.inmo.tgbotapi.types.reactions.Reaction
import dev.inmo.tgbotapi.utils.RiskFeature
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import pl.kvgx12.wiertarbot.connector.ConnectorContextServer
import pl.kvgx12.wiertarbot.connector.DelegatedCommandInvoker
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.proto.Response
import pl.kvgx12.wiertarbot.proto.connector.*
import pl.kvgx12.wiertarbot.proto.connector.Empty
import kotlin.io.path.Path
import kotlin.io.path.readBytes

class TelegramContext(
    private val connector: TelegramConnector,
    delegatedCommandInvoker: DelegatedCommandInvoker
) : ConnectorContextServer(ConnectorType.TELEGRAM, delegatedCommandInvoker) {
    private suspend inline fun <T : Any> execute(request: Request<T>) = connector.bot.execute(request)

    private fun chatId(string: String) = Json.decodeFromString<ChatIdentifier>(string)

    private fun Response.replyParameters(chatId: ChatIdentifier) = replyToId?.toLongOrNull()?.let {
        ReplyParameters(chatIdentifier = chatId, messageId = MessageId(it))
    }

    private fun Mention.toEntity(text: String) =
        RawChatId(threadId.toLong())
            .mentionTextSource(text.substring(offset, offset + length))

    private fun Response.buildEntities() = buildList {
        if (text == null) return@buildList

        mentionsList?.let { mentions ->
            val processed = mentions
                .sortedBy { it.offset }
                .fold(0) { acc, mention ->
                    if (acc < mention.offset) {

                        add(regularTextSource(text.substring(acc, mention.offset)))
                    }
                    add(mention.toEntity(text))

                    mention.offset + mention.length
                }

            if (processed < text.length) {
                add(regularTextSource(text.substring(processed, text.length)))
            }
        } ?: add(regularTextSource(text))
    }

    @OptIn(RiskFeature::class)
    private suspend fun sendFiles(response: Response, files: List<UploadedFile>) = coroutineScope {
        val chatId = chatId(response.event.threadId)
        val replyToId = response.replyParameters(chatId)

        if (files.size == 1) {
            val uploadedFile = files.first()
            val mimeType = uploadedFile.mimeType
            val file = uploadedFile.asMultipartFile()
            val entities = response.buildEntities()

            val request = when { // TODO: mimetype as enum?
                mimeType.startsWith("image") -> SendPhoto(
                    chatId,
                    file,
                    replyParameters = replyToId,
                    entities = entities,
                )

                mimeType.startsWith("video") -> SendVideo(
                    chatId,
                    file,
                    replyParameters = replyToId,
                    entities = entities,
                )

                mimeType.startsWith("audio") -> SendAudio(
                    chatId,
                    file,
                    replyParameters = replyToId,
                    entities = entities,
                )

                else -> SendDocument(chatId, file, replyParameters = replyToId, entities = entities)
            }

            execute(request)
            return@coroutineScope true
        } else if (files.size >= 2) {
            @Suppress("UNCHECKED_CAST")
            execute(
                SendMediaGroup<MediaGroupPartContent>(
                    chatId,
                    files.map(::fileToTelegramContent) as List<MediaGroupMemberTelegramMedia>,
                    replyParameters = replyToId,
                ),
            )
        }

        false
    }

    private fun UploadedFile.asMultipartFile() =
        MultipartFile(id) { ByteReadPacket(content!!.asReadOnlyByteBuffer()) }

    private fun fileToTelegramContent(file: UploadedFile): TelegramMedia {
        val mimeType = file.mimeType
        val multipartFile = file.asMultipartFile()

        return when { // TODO: mimetype as enum?
            mimeType.startsWith("audio") -> TelegramMediaAudio(multipartFile)
            mimeType.startsWith("video") -> TelegramMediaVideo(multipartFile)
            mimeType.startsWith("image") -> TelegramMediaPhoto(multipartFile)
            else -> TelegramMediaDocument(multipartFile)
        }
    }

    override suspend fun send(request: Response): SendResponse {
        if (!request.filesList.isNullOrEmpty() && sendFiles(request, request.filesList)) {
            return sendResponse {
                messageId = "" // TODO
            }
        }

        if (!request.text.isNullOrEmpty()) {
            val chatId = chatId(request.event.threadId)
            val result = execute(
                SendTextMessage(
                    chatId,
                    replyParameters = request.replyParameters(chatId),
                    entities = request.buildEntities(),
                ),
            )

            return sendResponse {
                messageId = result.messageId.long.toString()
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
        val chat = execute(GetChat(chatId(request.threadId)))

        return fetchThreadResponse {
            thread = threadData {
                id = request.threadId
                name = when (chat) {
                    is PublicChat -> chat.title
                    is UsernameChat -> chat.username?.withoutAt.orEmpty()
                    else -> ""
                }
                participants += when (chat) {
                    is ExtendedChatWithUsername -> chat.activeUsernames.mapNotNull { it.threadId?.toString() }
                    else -> emptyList()
                }
            }
        }
    }

    override suspend fun fetchImageUrl(request: FetchImageUrlRequest): FetchImageUrlResponse = fetchImageUrlResponse {
        url = execute(GetFile(FileId(request.id)))
            .fullUrl(connector.keeper)
    }

    override suspend fun sendText(request: SendTextRequest): Empty {
        execute(SendTextMessage(chatId(request.event.threadId), text = request.text))

        return Empty.getDefaultInstance()
    }

    override suspend fun reactToMessage(request: ReactToMessageRequest): Empty {
        execute(
            SetMessageReactions(
                chatId = chatId(request.event.threadId),
                messageId = MessageId(request.event.messageId.toLong()),
                reactions = listOf(Reaction.Emoji(request.reaction)),
            ),
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
