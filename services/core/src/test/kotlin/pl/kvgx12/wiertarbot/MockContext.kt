package pl.kvgx12.wiertarbot

import pl.kvgx12.wiertarbot.connector.*
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.utils.getLogger


class MockContext(
    interpreter: Interpreter
) : ConnectorContext(ConnectorType.FB) {
    private val log = getLogger()

    override suspend fun getBotId(): String = "test"

    override suspend fun sendResponse(response: Response) {
    }

    override suspend fun uploadRaw(files: List<FileData>, voiceClip: Boolean): List<UploadedFile> {
        TODO("Not yet implemented")
    }

    override suspend fun fetchThread(threadId: String): ThreadData {
        TODO("Not yet implemented")
    }

    override suspend fun fetchImageUrl(imageId: String): String {
        TODO("Not yet implemented")
    }

    override suspend fun sendText(event: MessageEvent, text: String) {
        TODO("Not yet implemented")
    }

    override suspend fun reactToMessage(event: MessageEvent, reaction: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun fetchRepliedTo(event: MessageEvent): MessageEvent? {
        TODO("Not yet implemented")
    }

    override suspend fun upload(files: List<String>, voiceClip: Boolean): List<UploadedFile>? {
        TODO("Not yet implemented")
    }
}
