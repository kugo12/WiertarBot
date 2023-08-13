package pl.kvgx12.wiertarbot.connector

import io.grpc.ManagedChannel
import pl.kvgx12.wiertarbot.proto.FileData
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.Response
import pl.kvgx12.wiertarbot.proto.connector.*
import pl.kvgx12.wiertarbot.proto.connector.ConnectorContextGrpcKt.ConnectorContextCoroutineStub
import java.io.Closeable
import java.util.concurrent.TimeUnit


class ConnectorContextClient(private val channel: ManagedChannel) : Closeable {
    private val stub = ConnectorContextCoroutineStub(channel)

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }


    suspend fun upload(file: String, voiceClip: Boolean = false) = stub.upload(
        uploadRequest {
            files += file
            this.voiceClip = voiceClip
        },
    ).filesList

    suspend fun upload(files: List<String>, voiceClip: Boolean = false) = stub.upload(
        uploadRequest {
            this.files += files
            this.voiceClip = voiceClip
        },
    ).filesList

    suspend fun uploadRaw(file: FileData, voiceClip: Boolean = false) = stub.uploadRaw(
        uploadRawRequest {
            files += file
            this.voiceClip = voiceClip
        },
    ).filesList

    suspend fun uploadRaw(files: List<FileData>, voiceClip: Boolean = false) = stub.uploadRaw(
        uploadRawRequest {
            this.files += files
            this.voiceClip = voiceClip
        },
    ).filesList

    suspend fun sendResponse(response: Response) {
        stub.sendResponse(response)
    }

    suspend fun fetchThread(threadId: String) = stub.fetchThread(
        fetchThreadRequest {
            this.threadId = threadId
        },
    ).threadOrNull

    suspend fun fetchImageUrl(id: String) = stub.fetchImageUrl(
        fetchImageUrlRequest {
            this.id = id
        },
    ).url

    suspend fun sendText(event: MessageEvent, text: String) {
        stub.sendText(
            sendTextRequest {
                this.event = event
                this.text = text
            },
        )
    }

    suspend fun reactToMessage(event: MessageEvent, reaction: String) {
        stub.reactToMessage(
            reactToMessageRequest {
                this.event = event
                this.reaction = reaction
            },
        )
    }

    suspend fun fetchRepliedTo(event: MessageEvent) = stub.fetchRepliedTo(
        fetchRepliedToRequest {
            this.event = event
        },
    ).eventOrNull

    suspend fun delegatedCommand(command: String, event: MessageEvent) {
        stub.delegatedCommand(
            delegatedCommandRequest {
                this.command = command
                this.event = event
            },
        )
    }
}
