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
import pl.kvgx12.wiertarbot.connector.ConnectorContextServer
import pl.kvgx12.wiertarbot.connectors.fb.FBKtConnector.Companion.toGeneric
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.proto.connector.*
import pl.kvgx12.wiertarbot.utils.contentType
import pl.kvgx12.wiertarbot.utils.proto.isGroup
import kotlin.io.path.Path
import kotlin.io.path.readBytes
import pl.kvgx12.fbchat.requests.FileData as FBFileData

class FBKtContext(
    private val session: Session,
) : ConnectorContextServer(ConnectorType.FB) {
    private inline val MessageEvent.thread get() = if (isGroup) GroupId(threadId) else UserId(threadId)

    override suspend fun sendResponse(request: Response): Empty {
        val thread = request.event.thread
        session.sendMessage(
            thread = thread,
            text = request.text,
            files = request.filesList
                .map { it.id to it.mimeType },
            replyTo = request.replyToId?.let { MessageId(thread, it) },
            mentions = request.mentionsList.map {
                Mention(UserId(it.threadId), offset = it.offset, length = it.length)
            },
        )

        return Empty.getDefaultInstance()
    }

    override suspend fun uploadRaw(request: UploadRawRequest): UploadResponse {
        val fileData = session.upload(
            request.filesList.map {
                FBFileData(
                    filename = it.uri,
                    channel = ChannelProvider(it.content.size().toLong()) { ByteReadChannel(it.content.asReadOnlyByteBuffer()) },
                    contentType = ContentType.parse(it.mimeType),
                )
            },
        )

        return uploadResponse {
            files += fileData.map {
                uploadedFile {
                    id = it.first
                    mimeType = it.second
                }
            }
        }
    }

    override suspend fun fetchThread(request: FetchThreadRequest): FetchThreadResponse {
        val thread = session.fetch(UnknownThread(request.threadId))

        checkNotNull(thread) // TODO

        return fetchThreadResponse {
            this.thread = when (thread) {
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
    }

    override suspend fun fetchImageUrl(request: FetchImageUrlRequest): FetchImageUrlResponse = fetchImageUrlResponse {
        url = session.fetchImageUrl(request.id)
    }

    override suspend fun sendText(request: SendTextRequest): Empty {
        session.sendMessage(
            thread = request.event.thread,
            text = request.text,
        )
        return Empty.getDefaultInstance()
    }

    override suspend fun reactToMessage(request: ReactToMessageRequest): Empty {
        MessageId(request.event.thread, request.event.externalId)
            .react(session, request.reaction)

        return Empty.getDefaultInstance()
    }

    override suspend fun fetchRepliedTo(request: FetchRepliedToRequest): FetchRepliedToResponse {
        if (request.event.replyToId.isNullOrEmpty()) {
            return fetchRepliedToResponse {}
        }

        val message = session.fetch(
            MessageId(request.event.thread, request.event.replyToId),
        )

        return fetchRepliedToResponse {
            event = messageEvent {
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
    }

    override suspend fun upload(request: UploadRequest): UploadResponse = coroutineScope {
        uploadRaw(
            uploadRawRequest {
                files += request.filesList
                    .map { async { downloadFile(it) } }
                    .awaitAll()
            },
        )
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
