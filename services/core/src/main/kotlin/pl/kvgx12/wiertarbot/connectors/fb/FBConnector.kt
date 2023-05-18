package pl.kvgx12.wiertarbot.connectors.fb

import jep.python.PyCallable
import jep.python.PyObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import pl.kvgx12.wiertarbot.connector.Connector
import pl.kvgx12.wiertarbot.events.*
import pl.kvgx12.wiertarbot.utils.execute
import pl.kvgx12.wiertarbot.python.*
import pl.kvgx12.wiertarbot.utils.getLogger

class FBConnector(
    private val interpreter: Interpreter,
) : Connector {
    private lateinit var pyConnector: PyConnector
    private var contextProxy: FBConnectorContext? = null
    private val log = getLogger()

    init {
        runBlocking {
            interpreter {
                execute(
                    """
                from WiertarBot.connectors.fb import FBConnector, FBEventDispatcher
                from WiertarBot import fbchat
                """.trimIndent()
                )
                pyConnector = get<PyCallable1<PyCallable1<PyObject, Unit>, PyObject>>("FBConnector")
                    .__call__ {
                        contextProxy = FBConnectorContext(interpreter, it.proxy())
                    }.proxy()
            }
        }
    }

    override fun run(): Flow<Event> = callbackFlow {
        val scope = CoroutineScope(interpreter.context + SupervisorJob())
        val task = interpreter {
            pyConnector.login().pyAwait()
            addTask.callAs(
                PyObject::class.java,
                pyConnector.run {
                    scope.launch {
                        fbEventIntoGenericEvent(contextProxy!!, it)?.let(::trySend)
                    }
                    Unit
                }
            )
        }

        awaitClose {
            scope.cancel()
            runBlocking {
                interpreter {
                    task.get<PyCallable>("cancel").call()
                }
            }
        }
    }

    private suspend fun fbEventIntoGenericEvent(context: FBConnectorContext, event: PyObject): Event? = interpreter {
        val type = event.className()

        when (type) {
            "MessageEvent", "MessageReplyEvent" -> FBToGeneric.messageEvent(context, event)
            else -> null
        }
    }

    object FBToGeneric {
        fun messageEvent(context: FBConnectorContext, pyObject: PyObject): MessageEvent {
            val message = pyObject.pyGet("message")

            return MessageEvent(
                context = context,
                text = message.get("text") ?: "",
                authorId = pyObject.pyGet("author").get("id"),
                threadId = pyObject.pyGet("thread").get("id"),
                at = message.pyGet("created_at").get<PyCallable>("timestamp").call().let { (it as Double).toLong() },
                mentions = message.get<List<PyObject>>("mentions").map(FBToGeneric::mention),
                externalId = message.get("id"),
                replyToId = message.get("reply_to_id"),
                attachments = message.get<List<PyObject>>("attachments").map(FBToGeneric::attachment)
            )
        }

        fun attachment(pyObject: PyObject) = when (pyObject.className()) {
            "ImageAttachment" -> ImageAttachment(
                id = pyObject.get("id"),
                width = pyObject.get("width"),
                height = pyObject.get("height"),
                originalExtension = pyObject.get("original_extension"),
                isAnimated = pyObject.get("is_animated")
            )

            else -> Attachment(id = pyObject.get("id"))
        }

        fun mention(pyObject: PyObject) = Mention(
            threadId = pyObject.get("thread_id"),
            offset = pyObject.get("offset"),
            length = pyObject.get("length")
        )
    }

    interface PyConnector {
        fun run(callback: PyCallable1<PyObject, Unit>): PyObject
        fun login(): PyObject
        fun get_context(): PyObject
    }
}
