package pl.kvgx12.wiertarbot.connectors

import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.chat.get.GetChat
import dev.inmo.tgbotapi.requests.get.GetFile
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.requests.send.media.*
import dev.inmo.tgbotapi.types.*
import dev.inmo.tgbotapi.types.chat.ExtendedChatWithUsername
import dev.inmo.tgbotapi.types.chat.PublicChat
import dev.inmo.tgbotapi.types.chat.UsernameChat
import dev.inmo.tgbotapi.types.files.*
import dev.inmo.tgbotapi.types.media.*
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyReplyMessage
import dev.inmo.tgbotapi.types.message.content.MediaGroupPartContent
import dev.inmo.tgbotapi.types.message.textsources.mention
import dev.inmo.tgbotapi.types.message.textsources.regular
import dev.inmo.tgbotapi.utils.RiskFeature
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import pl.kvgx12.wiertarbot.connector.*
import pl.kvgx12.wiertarbot.events.Mention
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.python.Interpreter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readBytes

class TelegramContext(
    private val connector: TelegramConnector,
    private val message: Message,
    interpreter: Interpreter,
) : ConnectorContext(ConnectorType.Telegram, interpreter) {
    private suspend inline fun <T : Any> execute(request: Request<T>) = connector.bot.execute(request)

    private fun Mention.toEntity(text: String) =
        UserId(threadId.toLong())
            .mention(text.substring(offset, offset + length))

    private fun Response.buildEntities() = buildList {
        if (text == null) return@buildList

        mentions?.let { mentions ->
            val processed = mentions
                .sortedBy { it.offset }
                .fold(0) { acc, mention ->
                    if (acc < mention.offset) {
                        add(regular(text.substring(acc, mention.offset)))
                    }
                    add(mention.toEntity(text))

                    mention.offset + mention.length
                }

            if (processed < text.length)
                add(regular(text.substring(processed, text.length)))
        } ?: add(regular(text))
    }

    @OptIn(RiskFeature::class)
    private suspend fun sendFiles(response: Response, files: List<UploadedFile>) = coroutineScope {
        val chatId = message.chat.id
        val replyToId = response.replyToId?.toLongOrNull()

        if (files.size == 1) {
            val uploadedFile = files.first()
            val mimeType = uploadedFile.mimeType
            val file = uploadedFile.asMultipartFile()
            val entities = response.buildEntities()

            val request = when {  // TODO: mimetype as enum?
                mimeType.startsWith("image") -> SendPhoto(
                    chatId,
                    file,
                    replyToMessageId = replyToId,
                    entities = entities
                )

                mimeType.startsWith("video") -> SendVideo(
                    chatId,
                    file,
                    replyToMessageId = replyToId,
                    entities = entities
                )

                mimeType.startsWith("audio") -> SendAudio(
                    chatId,
                    file,
                    replyToMessageId = replyToId,
                    entities = entities
                )

                else -> SendDocument(chatId, file, replyToMessageId = replyToId, entities = entities)
            }

            execute(request)
            return@coroutineScope true
        } else if (files.size >= 2) {
            execute(
                SendMediaGroup<MediaGroupPartContent>(
                    chatId,
                    files.map(::fileToTelegramContent) as List<MediaGroupMemberTelegramMedia>,
                    replyToMessageId = replyToId
                )
            )
        }

        false
    }

    private fun UploadedFile.asMultipartFile() = MultipartFile(id) { ByteReadPacket(content!!) }

    private fun fileToTelegramContent(file: UploadedFile): TelegramMedia {
        val mimeType = file.mimeType
        val file = file.asMultipartFile()

        return when {  // TODO: mimetype as enum?
            mimeType.startsWith("audio") -> TelegramMediaAudio(file)
            mimeType.startsWith("video") -> TelegramMediaVideo(file)
            mimeType.startsWith("image") -> TelegramMediaPhoto(file)
            else -> TelegramMediaDocument(file)
        }
    }

    override suspend fun getBotId(): String = connector.me.id.chatId.toString()

    override suspend fun sendResponse(response: Response) {
        if (!response.files.isNullOrEmpty() && sendFiles(response, response.files))
            return

        if (!response.text.isNullOrEmpty()) {
            execute(
                SendTextMessage(
                    message.chat.id,
                    replyToMessageId = response.replyToId?.toLongOrNull(),
                    entities = response.buildEntities()
                )
            )
        }
    }

    override suspend fun uploadRaw(files: List<FileData>, voiceClip: Boolean): List<UploadedFile> =
        files.map { UploadedFile(it.uri, it.mediaType, it.content) }

    override suspend fun fetchThread(threadId: String): ThreadData {
        val chat = execute(GetChat(ChatId(threadId.toLong())))

        return ThreadData(
            id = threadId,
            name = when (chat) {
                is PublicChat -> chat.title
                is UsernameChat -> chat.username?.usernameWithoutAt ?: ""
                else -> ""
            },
            messageCount = null,
            participants = when (chat) {
                is ExtendedChatWithUsername -> chat.activeUsernames.mapNotNull { it.threadId?.toString() }
                else -> emptyList()
            }
        )
    }

    override suspend fun fetchImageUrl(imageId: String) =
        execute(GetFile(FileId(imageId)))
            .fullUrl(connector.keeper)

    override suspend fun sendText(event: MessageEvent, text: String) {
        execute(SendTextMessage(ChatId(event.threadId.toLong()), text = text))
    }

    // https://bugs.telegram.org/c/12710
    override suspend fun reactToMessage(event: MessageEvent, reaction: String?) = Unit

    override suspend fun fetchRepliedTo(event: MessageEvent) =
        (message as? PossiblyReplyMessage)
            ?.replyTo
            ?.let(connector::convert)

    override suspend fun upload(files: List<String>, voiceClip: Boolean) = coroutineScope {
        files.map { async { downloadFile(it) } }
            .awaitAll()
    }

    companion object {
        suspend fun Path.contentType() = withContext(Dispatchers.IO) { Files.probeContentType(this@contentType) }

        private suspend fun downloadFile(urlString: String): UploadedFile {
            val url = Url(urlString)
            if (!urlString.startsWith(url.protocol.name)) {
                return withContext(Dispatchers.IO) {
                    val path = Path(urlString)
                    val content = path.readBytes()
                    val contentType = path.contentType()

                    UploadedFile(urlString, contentType, content)
                }
            }

            val response = client.get(url)
            val contentType = response.contentType()
                ?.let { "${it.contentType}/${it.contentSubtype}" }
                ?: Path(urlString).contentType()

            require(response.status.value in 200..299) {
                "GET $url status ${response.status}"
            }
            val content = response.body<ByteArray>()

            return UploadedFile(url.fullPath, contentType, content)
        }

        private val client = HttpClient { }
    }
}
