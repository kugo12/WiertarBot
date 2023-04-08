package pl.kvgx12.wiertarbot.connector

import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response

abstract class ConnectorContext(
    val connectorType: ConnectorType,
) {
    abstract suspend fun getBotId(): String
    abstract suspend fun sendResponse(response: Response)
    abstract suspend fun uploadRaw(files: List<FileData>, voiceClip: Boolean = false): List<UploadedFile>
    abstract suspend fun fetchThread(threadId: String): ThreadData
    abstract suspend fun fetchImageUrl(imageId: String): String
    abstract suspend fun sendText(event: MessageEvent, text: String)
    abstract suspend fun reactToMessage(event: MessageEvent, reaction: String?)
    abstract suspend fun fetchRepliedTo(event: MessageEvent): MessageEvent?
    abstract suspend fun upload(files: List<String>, voiceClip: Boolean = false): List<UploadedFile>?

    suspend inline fun upload(file: String, voiceClip: Boolean = false) = upload(listOf(file), voiceClip)
}
