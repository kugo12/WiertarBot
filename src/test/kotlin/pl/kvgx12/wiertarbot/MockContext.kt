package pl.kvgx12.wiertarbot

import jep.python.PyObject
import pl.kvgx12.wiertarbot.connector.ConnectorContext
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.utils.getLogger


class MockContext(
    interpreter: Interpreter
) : ConnectorContext(interpreter) {
    private val log = getLogger()
    override suspend fun sendResponse(response: Response) {
    }

    override suspend fun uploadRaw(files: Iterable<PyObject>, voiceClip: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun fetchThread(threadId: String): PyObject {
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

    override suspend fun fetchRepliedTo(event: MessageEvent): PyObject? {
        TODO("Not yet implemented")
    }

    override suspend fun saveAttachment(attachment: PyObject) {
        TODO("Not yet implemented")
    }

    override suspend fun upload(files: Iterable<String>, voiceClip: Boolean): Iterable<PyObject>? {
        TODO("Not yet implemented")
    }
}