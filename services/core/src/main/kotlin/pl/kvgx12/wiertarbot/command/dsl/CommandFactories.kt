package pl.kvgx12.wiertarbot.command.dsl

import com.sksamuel.scrimage.ImmutableImage
import pl.kvgx12.wiertarbot.command.Command
import pl.kvgx12.wiertarbot.command.CommandData
import pl.kvgx12.wiertarbot.command.ImageEdit
import pl.kvgx12.wiertarbot.command.ImageEditCommand
import pl.kvgx12.wiertarbot.connector.ConnectorType
import pl.kvgx12.wiertarbot.connector.FileData
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.utils.toImmutableImage
import java.awt.image.BufferedImage
import java.util.*

inline fun CommandDsl.imageEdit(
    crossinline func: ImageEdit<BufferedImage>,
) = object : ImageEditCommand(help!!, name, aliases, availableIn) {
    override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
        func(state, image)
}

inline fun CommandDsl.immutableImageEdit(
    crossinline func: ImageEdit<ImmutableImage>,
) = object : ImageEditCommand(help!!, name, aliases, availableIn) {
    override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
        func(state, image.toImmutableImage()).awt()
}

inline fun CommandDsl.generic(
    crossinline func: suspend (MessageEvent) -> Response?,
): CommandData {
    val help = help!!
    val name = name
    val aliases = aliases
    val availableIn = availableIn

    return object : Command {
        override val help: String get() = help
        override val name: String get() = name
        override val aliases: List<String> get() = aliases
        override val availableIn: EnumSet<ConnectorType> get() = availableIn

        override suspend fun process(event: MessageEvent): Response? = func(event)
    }
}

inline fun CommandDsl.text(
    crossinline func: suspend (MessageEvent) -> String?,
) = generic { Response(it, text = func(it)) }

inline fun CommandDsl.files(
    voiceClip: Boolean = false,
    crossinline func: suspend (MessageEvent) -> List<String>,
) = generic { Response(it, files = it.context.upload(func(it), voiceClip)) }

inline fun CommandDsl.rawFiles(
    voiceClip: Boolean = false,
    crossinline func: suspend (MessageEvent) -> List<FileData>,
) = generic { Response(it, files = it.context.uploadRaw(func(it), voiceClip)) }

inline fun CommandDsl.file(
    voiceClip: Boolean = false,
    crossinline func: suspend (MessageEvent) -> String,
) = generic { Response(it, files = it.context.upload(func(it), voiceClip)) }

inline fun CommandDsl.rawFile(
    voiceClip: Boolean = false,
    crossinline func: suspend (MessageEvent) -> FileData,
) = generic { Response(it, files = it.context.uploadRaw(listOf(func(it)), voiceClip)) }
