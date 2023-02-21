package pl.kvgx12.wiertarbot.connectors

import jep.python.PyObject
import pl.kvgx12.wiertarbot.connector.ConnectorContext
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.python.Interpreter

class FBConnectorContext(
    interpreter: Interpreter,
    private val pyContext: PyContext,
) : ConnectorContext(interpreter) {
    override suspend fun getBotId() = interpreter {
        pyContext.get_bot_id()
    }

    override suspend fun sendResponse(response: Response) {
        interpreter {
            pyContext.send_response(response).pyAwait()
        }
    }

    override suspend fun uploadRaw(files: Iterable<PyObject>, voiceClip: Boolean) {
        interpreter {
            pyContext.upload_raw(files, voiceClip).pyAwait()
        }
    }

    override suspend fun fetchThread(threadId: String): PyObject = interpreter {
        pyContext.fetch_thread(threadId).pyAwait() as PyObject
    }

    override suspend fun fetchImageUrl(imageId: String): String = interpreter {
        pyContext.fetch_image_url(imageId).pyAwait() as String
    }

    override suspend fun sendText(event: MessageEvent, text: String) {
        interpreter {
            pyContext.send_text(event, text).pyAwait()
        }
    }

    override suspend fun reactToMessage(event: MessageEvent, reaction: String?) {
        interpreter {
            pyContext.react_to_message(event, reaction).pyAwait()
        }
    }

    override suspend fun fetchRepliedTo(event: MessageEvent): PyObject? = interpreter {
        pyContext.fetch_replied_to(event).pyAwait() as? PyObject
    }

    override suspend fun saveAttachment(attachment: PyObject) {
        interpreter {
            pyContext.save_attachment(attachment).pyAwait()
        }
    }

    override suspend fun upload(files: Iterable<String>, voiceClip: Boolean): Iterable<PyObject>? = interpreter {
        pyContext.upload(files, voiceClip).pyAwait() as? Iterable<PyObject>
    }

    @Suppress("FunctionName")
    interface PyContext {
        fun send_response(response: Response): PyObject
        fun upload_raw(files: Iterable<PyObject>, voiceClip: Boolean): PyObject
        fun fetch_thread(id: String): PyObject
        fun fetch_image_url(imageId: String): PyObject
        fun send_text(event: MessageEvent, text: String): PyObject
        fun react_to_message(event: MessageEvent, reaction: String?): PyObject
        fun fetch_replied_to(event: MessageEvent): PyObject
        fun save_attachment(attachment: PyObject): PyObject
        fun upload(files: Iterable<String>, voiceClip: Boolean): PyObject
        fun get_bot_id(): String
    }
}