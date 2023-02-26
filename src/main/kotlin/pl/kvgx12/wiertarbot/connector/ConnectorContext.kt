package pl.kvgx12.wiertarbot.connector

import jep.python.PyObject
import pl.kvgx12.wiertarbot.events.Attachment
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.python.PyFuture

abstract class ConnectorContext(
    val interpreter: Interpreter
) {
    abstract suspend fun getBotId(): String
    abstract suspend fun sendResponse(response: Response)
    abstract suspend fun uploadRaw(files: List<FileData>, voiceClip: Boolean = false): List<UploadedFile>
    abstract suspend fun fetchThread(threadId: String): PyObject
    abstract suspend fun fetchImageUrl(imageId: String): String
    abstract suspend fun sendText(event: MessageEvent, text: String)
    abstract suspend fun reactToMessage(event: MessageEvent, reaction: String?)
    abstract suspend fun fetchRepliedTo(event: MessageEvent): PyObject?
    abstract suspend fun saveAttachment(attachment: PyObject)
    abstract suspend fun upload(files: List<String>, voiceClip: Boolean = false): List<UploadedFile>?

    inline fun <T> intoFuture(crossinline f: suspend () -> T): PyFuture<T> = interpreter.wrapIntoFuture { f() }

    fun pySendResponse(response: Response) = intoFuture { sendResponse(response) }
    fun pyUploadRaw(files: List<FileData>, voiceClip: Boolean = false) = intoFuture { uploadRaw(files, voiceClip) }
    fun pyFetchThread(threadId: String) = intoFuture { fetchThread(threadId) }
    fun pyFetchImageUrl(imageId: String) = intoFuture { fetchImageUrl(imageId) }
    fun pySendText(event: MessageEvent, text: String) = intoFuture { sendText(event, text) }
    fun pyReactToMessage(event: MessageEvent, reaction: String?) = intoFuture { reactToMessage(event, reaction) }
    fun pyFetchRepliedTo(event: MessageEvent) = intoFuture { fetchRepliedTo(event) }
    fun pySaveAttachment(attachment: PyObject) = intoFuture { saveAttachment(attachment) }
    fun pyUpload(files: List<String>, voiceClip: Boolean = false) = intoFuture { upload(files, voiceClip) }
}