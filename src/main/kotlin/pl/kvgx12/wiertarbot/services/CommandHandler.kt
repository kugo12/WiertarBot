package pl.kvgx12.wiertarbot.services

import jep.python.PyCallable
import pl.kvgx12.wiertarbot.command.Command
import pl.kvgx12.wiertarbot.command.CommandData
import pl.kvgx12.wiertarbot.command.ImageEditCommand
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.python.get

sealed interface CommandHandler {
    @JvmInline
    value class PyGeneric(val f: PyCallable) : CommandHandler {
        suspend inline operator fun invoke(
            interpreter: Interpreter,
            event: MessageEvent
        ) = interpreter {
            f.intoCoroutine(arrayOf(event))
        } as? Response

        suspend inline fun help(interpreter: Interpreter): String = interpreter {
            f.get("__doc__")
        }
    }

    @JvmInline
    value class KtGeneric(val f: Command) : CommandHandler {
        suspend inline operator fun invoke(event: MessageEvent) =
            f.process(event)
    }

    @JvmInline
    value class ImageEdit(val command: ImageEditCommand) : CommandHandler {
        suspend inline operator fun invoke(event: MessageEvent) =
            command.check(event)
    }
}

fun CommandData.toHandler() = when (this) {
    is Command -> CommandHandler.KtGeneric(this)
    is ImageEditCommand -> CommandHandler.ImageEdit(this)
}

suspend fun CommandHandler.help(interpreter: Interpreter) = when (this) {
    is CommandHandler.ImageEdit -> command.help
    is CommandHandler.KtGeneric -> f.help
    is CommandHandler.PyGeneric -> help(interpreter)
}
