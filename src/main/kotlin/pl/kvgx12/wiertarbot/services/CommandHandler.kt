package pl.kvgx12.wiertarbot.services

import pl.kvgx12.wiertarbot.command.Command
import pl.kvgx12.wiertarbot.command.CommandData
import pl.kvgx12.wiertarbot.command.ImageEditCommand
import pl.kvgx12.wiertarbot.events.MessageEvent

sealed interface CommandHandler {
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

fun CommandHandler.help() = when (this) {
    is CommandHandler.ImageEdit -> command.help
    is CommandHandler.KtGeneric -> f.help
}
