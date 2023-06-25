package pl.kvgx12.wiertarbot.connectors.fb

import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.requests.*
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.wiertarbot.connector.*
import pl.kvgx12.wiertarbot.connector.ThreadData
import pl.kvgx12.wiertarbot.connectors.fb.FBKtConnector.Companion.toGeneric
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.utils.contentType
import kotlin.io.path.Path
import kotlin.io.path.readBytes
import pl.kvgx12.fbchat.requests.FileData as FBFileData

class FBKtContext(
    private val session: Session,
) : ConnectorContext(ConnectorType.FB) {
    private inline val MessageEvent.thread get() = if (isGroup) GroupId(threadId) else UserId(threadId)

    override suspend fun getBotId(): String = session.userId

    override suspend fun sendResponse(response: Response) {
        val thread = response.event.thread
        session.sendMessage(
            thread = thread,
            text = response.text,
            files = response.files.orEmpty()
                .map { it.id to it.mimeType },
            replyTo = response.replyToId?.let { MessageId(thread, it) },
            mentions = response.mentions.orEmpty().map {
                Mention(UserId(it.threadId), offset = it.offset, length = it.length)
            }
        )
    }

    override suspend fun uploadRaw(files: List<FileData>, voiceClip: Boolean): List<UploadedFile> {
        val files = session.upload(files.map {
            FBFileData(
                filename = it.uri,
                channel = ChannelProvider(it.content.size.toLong()) { ByteReadChannel(it.content) },
                contentType = ContentType.parse(it.mediaType)
            )
        })

        return files.map { UploadedFile(id = it.first, mimeType = it.second) }
    }

    override suspend fun fetchThread(threadId: String): ThreadData {
        val thread = session.fetch(UnknownThread(threadId))

        return when (thread) {
            is GroupData -> ThreadData(
                id = thread.id,
                name = thread.name ?: "",
                messageCount = thread.messageCount?.toLong(),
                participants = thread.participants.map { it.id }
            )

            is PageData -> ThreadData(
                id = thread.id,
                name = thread.name,
                messageCount = thread.messageCount?.toLong(),
                participants = listOf(thread.id, session.userId)
            )

            is UserData -> ThreadData(
                id = thread.id,
                name = thread.name,
                messageCount = thread.messageCount?.toLong(),
                participants = listOf(thread.id, session.userId)
            )
        }
    }

    override suspend fun fetchImageUrl(imageId: String): String =
        session.fetchImageUrl(imageId)

    override suspend fun sendText(event: MessageEvent, text: String) {
        session.sendMessage(
            thread = event.thread, text = text
        )
    }

    override suspend fun reactToMessage(event: MessageEvent, reaction: String?) {
        MessageId(event.thread, event.externalId)
            .react(session, reaction)
    }

    override suspend fun fetchRepliedTo(event: MessageEvent): MessageEvent? {
        if (event.replyToId.isNullOrEmpty())
            return null

        val message = session.fetch(
            MessageId(event.thread, event.replyToId)
        )

        return MessageEvent(
            this,
            text = message.text ?: "",
            authorId = message.author.id,
            threadId = message.thread.id,
            at = message.createdAt!!,
            mentions = message.mentions.map { it.toGeneric() },
            externalId = message.id,
            replyToId = message.repliedTo?.id,
            attachments = message.attachments.map {
                it.toGeneric()
            },
        )
    }

    override suspend fun upload(files: List<String>, voiceClip: Boolean): List<UploadedFile>? = coroutineScope {
        val downloadedFiles = files.map { async { downloadFile(it) } }
            .awaitAll()

        uploadRaw(downloadedFiles, voiceClip)
    }

    private suspend fun downloadFile(urlString: String): FileData {
        val url = Url(urlString)
        if (!urlString.startsWith(url.protocol.name)) {
            return withContext(Dispatchers.IO) {
                val path = Path(urlString)
                val content = path.readBytes()
                val contentType = path.contentType()

                FileData(urlString, content, contentType)
            }
        }

        val response = session.get(url)
        val contentType = response.contentType()
            ?.let { "${it.contentType}/${it.contentSubtype}" }
            ?: Path(urlString).contentType()

        val content = response.body<ByteArray>()

        return FileData(url.fullPath, content, contentType)
    }
}
