package pl.kvgx12.wiertarbot.services

import jep.python.PyCallable
import jep.python.PyObject
import kotlinx.coroutines.*
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.config.SpecialCommandsConfiguration
import pl.kvgx12.wiertarbot.config.WiertarbotProperties
import pl.kvgx12.wiertarbot.events.Attachment
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.execute
import pl.kvgx12.wiertarbot.python.*
import java.time.Instant
import java.util.Collections

sealed interface CommandHandler {
    @JvmInline
    value class Function(val f: PyCallable) : CommandHandler {
        suspend inline operator fun invoke(
            interpreter: Interpreter,
            event: MessageEvent
        ) = interpreter {
            f.intoCoroutine(arrayOf(event))
        } as? Response
    }

    @JvmInline
    value class ImageEditClass(val f: PyCallable) : CommandHandler {
        suspend inline operator fun invoke(
            interpreter: Interpreter,
            text: String
        ) = interpreter {
            f.callAs(PyObject::class.java, text).proxy<ImageEdit>()
        }
    }

    sealed interface ImageEdit {
        fun check(event: MessageEvent): PyObject
        fun get_image_from_attachments(event: MessageEvent, attachments: List<Attachment>): PyObject
        fun edit_and_send(event: MessageEvent, img: PyObject): PyObject
    }
}

@Service
class CommandService(
    private val permissionService: PermissionService,
    private val wiertarbotProperties: WiertarbotProperties,
    private val interpreter: Interpreter,
    specialCommandsConfiguration: SpecialCommandsConfiguration,
) {
    private val commands = mutableMapOf<String, CommandHandler>()
    private val special = specialCommandsConfiguration.commands
    private val aliases = mutableMapOf<String, String>()
    private val imageEditQueue = Collections.synchronizedMap(
        mutableMapOf<String, Pair<Long, CommandHandler.ImageEdit>>()
    )
    private val specialCommandsContext = Dispatchers.Default + SupervisorJob()

    init {
        runBlocking(interpreter.context) {
            registerPythonCommands()
            println(commands)
        }
    }

    fun getCommand(name: String) = aliases[name]?.let(commands::get)

    fun getCommands() = commands as Map<String, CommandHandler>

    suspend fun dispatch(event: MessageEvent) = coroutineScope {
        if (event.authorId == event.context.getBotId() || permissionService.isAuthorized(
                "banned",
                event.threadId,
                event.authorId
            )
        )
            return@coroutineScope

        if (event.text.isNotEmpty()) {
            if (event.text.startsWith(wiertarbotProperties.prefix)) {
                val commandName = event.text
                    .substringBefore(' ')
                    .drop(wiertarbotProperties.prefix.length)
                    .lowercase()
                    .let(aliases::get)

                if (commandName != null && permissionService.isAuthorized(
                        commandName,
                        event.threadId,
                        event.authorId
                    )
                ) {
                    when (val command = commands[commandName]) {
                        is CommandHandler.Function -> interpreter.launch {
                            command(interpreter, event)?.send()
                        }
                        is CommandHandler.ImageEditClass -> interpreter.launch {
                            val imageEdit = command(interpreter, event.text)
                            if (imageEdit.check(event).pyAwait() as Boolean) {
                                val queueId = "${event.threadId}_${event.authorId}"
                                imageEditQueue[queueId] = Instant.now().epochSecond to imageEdit
                            }
                        }

                        else -> {}
                    }
                }
            }

            launch(specialCommandsContext) {
                special.forEach { it(event) }
            }
        } else {
            val queueId = "${event.threadId}_${event.authorId}"
            val (time, edit) = imageEditQueue[queueId] ?: return@coroutineScope

            if (time + Constants.imageEditTimeout > Instant.now().epochSecond) {
                interpreter.launch {
                    edit.get_image_from_attachments(event, event.attachments)
                        .pyAwait()
                        ?.let {
                            edit.edit_and_send(event, it as PyObject)
                                .pyAwait()
                            imageEditQueue.remove(queueId)
                        }
                }
            } else imageEditQueue.remove(queueId)
        }
    }

    private suspend fun registerPythonCommands() {
        interpreter {
            dispatcherHook(wiertarBot.init_dispatcher())
        }
    }

    private fun dispatcherHook(dispatcher: PyObject) {
        aliases.putAll(dispatcher.get("_alias_of"))

        dispatcher.get<Map<String, PyObject>>("_commands")
            .forEach { (name, obj) ->
                val checked = with(interpreter.inspect) {
                    when {
                        iscoroutinefunction(obj) -> CommandHandler.Function(obj as PyCallable)
                        isclass(obj) -> CommandHandler.ImageEditClass(obj as PyCallable)
                        else -> throw IllegalArgumentException(
                            "Object has invalid type, command=$name object=$obj"
                        )
                    }
                }

                commands[name] = checked
            }

        initCommandPermissions()
    }

    @Async
    fun initCommandPermissions() {
        commands.forEach { (name, _) -> permissionService.initPermissionByCommand(name) }
    }
}