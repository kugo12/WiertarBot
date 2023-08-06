package pl.kvgx12.wiertarbot.connectors.fb

import com.google.protobuf.kotlin.toByteString
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.data.Mention
import pl.kvgx12.fbchat.requests.*
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.wiertarbot.connector.ConnectorContext
import pl.kvgx12.wiertarbot.connectors.fb.FBKtConnector.Companion.toGeneric
import pl.kvgx12.wiertarbot.utils.proto.isGroup
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.proto.ThreadData
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
            files = response.filesList
                .map { it.id to it.mimeType },
            replyTo = response.replyToId?.let { MessageId(thread, it) },
            mentions = response.mentionsList.map {
                Mention(UserId(it.threadId), offset = it.offset, length = it.length)
            },
        )
    }

    override suspend fun uploadRaw(files: List<FileData>, voiceClip: Boolean): List<UploadedFile> {
        val fileData = session.upload(
            files.map {
                FBFileData(
                    filename = it.uri,
                    channel = ChannelProvider(it.content.size().toLong()) { ByteReadChannel(it.content.asReadOnlyByteBuffer()) },
                    contentType = ContentType.parse(it.mimeType),
                )
            },
        )

        return fileData.map {
            uploadedFile {
                id = it.first
                mimeType = it.second
            }
        }
    }

    override suspend fun fetchThread(threadId: String): ThreadData {
        val thread = session.fetch(UnknownThread(threadId))

        checkNotNull(thread) // TODO

        return when (thread) {
            is GroupData -> threadData {
                id = thread.id
                name = thread.name.orEmpty()
                thread.messageCount?.toLong()?.let { messageCount = it }
                participants += thread.participants.map { it.id }
            }

            is PageData -> threadData {
                id = thread.id
                name = thread.name
                thread.messageCount?.toLong()?.let { messageCount = it }
                participants += listOf(thread.id, session.userId)
            }

            is UserData -> threadData {
                id = thread.id
                name = thread.name
                thread.messageCount?.toLong()?.let { messageCount = it }
                participants += listOf(thread.id, session.userId)
            }
        }
    }

    override suspend fun fetchImageUrl(imageId: String): String =
        session.fetchImageUrl(imageId)

    override suspend fun sendText(event: MessageEvent, text: String) {
        session.sendMessage(
            thread = event.thread,
            text = text,
        )
    }

    override suspend fun reactToMessage(event: MessageEvent, reaction: String?) {
        MessageId(event.thread, event.externalId)
            .react(session, reaction)
    }

    override suspend fun fetchRepliedTo(event: MessageEvent): MessageEvent? {
        if (event.replyToId.isNullOrEmpty()) {
            return null
        }

        val message = session.fetch(
            MessageId(event.thread, event.replyToId),
        )

        return messageEvent {
            text = message.text.orEmpty()
            authorId = message.author.id
            threadId = message.thread.id
            at = message.createdAt!!
            mentions.addAll(message.mentions.map { it.toGeneric() })
            externalId = message.id
            message.repliedTo?.id?.let { replyToId = it }
            attachments.addAll(message.attachments.map { it.toGeneric() })
        }
    }

    override suspend fun upload(files: List<String>, voiceClip: Boolean): List<UploadedFile> = coroutineScope {
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
                val contentType = path.contentType()!!

                fileData {
                    uri = urlString
                    mimeType = contentType
                    this.content = content.toByteString()
                }
            }
        }

        val response = session.get(url)
        val contentType = response.contentType()
            ?.let { "${it.contentType}/${it.contentSubtype}" }
            ?: Path(urlString).contentType()!!

        val content = response.body<ByteArray>()

        return fileData {
            uri = url.fullPath
            mimeType = contentType
            this.content = content.toByteString()
        }
    }
}
