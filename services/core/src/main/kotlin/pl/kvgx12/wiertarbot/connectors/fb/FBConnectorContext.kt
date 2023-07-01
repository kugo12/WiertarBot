package pl.kvgx12.wiertarbot.connectors.fb

import jep.python.PyObject
import pl.kvgx12.wiertarbot.connector.*
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.python.Interpreter

class FBConnectorContext(
    val interpreter: Interpreter,
    private val pyContext: PyContext,
) : ConnectorContext(ConnectorType.FB) {
    override suspend fun getBotId() = interpreter {
        pyContext.get_bot_id()
    }

    override suspend fun sendResponse(response: Response) {
        interpreter {
            pyContext.send_response(response).pyAwait()
        }
    }

    override suspend fun uploadRaw(files: List<FileData>, voiceClip: Boolean) = interpreter {
        @Suppress("UNCHECKED_CAST")
        pyContext.upload_raw(files, voiceClip).pyAwait() as List<UploadedFile>
    }

    override suspend fun fetchThread(threadId: String) = interpreter {
        pyContext.fetch_thread(threadId).pyAwait() as ThreadData
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

    override suspend fun fetchRepliedTo(event: MessageEvent) = interpreter {
        pyContext.fetch_replied_to(event).pyAwait() as? MessageEvent
    }

    override suspend fun upload(files: List<String>, voiceClip: Boolean) = interpreter {
        @Suppress("UNCHECKED_CAST")
        pyContext.upload(files, voiceClip).pyAwait() as? List<UploadedFile>
    }

    @Suppress("FunctionName")
    interface PyContext {
        fun send_response(response: Response): PyObject
        fun upload_raw(files: List<FileData>, voiceClip: Boolean): PyObject
        fun fetch_thread(id: String): PyObject
        fun fetch_image_url(imageId: String): PyObject
        fun send_text(event: MessageEvent, text: String): PyObject
        fun react_to_message(event: MessageEvent, reaction: String?): PyObject
        fun fetch_replied_to(event: MessageEvent): PyObject
        fun upload(files: List<String>, voiceClip: Boolean): PyObject
        fun get_bot_id(): String
    }
}
