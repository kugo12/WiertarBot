package pl.kvgx12.wiertarbot.command.dsl

import com.sksamuel.scrimage.ImmutableImage
import pl.kvgx12.wiertarbot.command.*
import pl.kvgx12.wiertarbot.connector.FileData
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.utils.toImmutableImage
import java.awt.image.BufferedImage

inline fun CommandDsl.imageEdit(
    crossinline func: ImageEdit<BufferedImage>,
) = metadata(
    object : ImageEditCommand() {
        override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
            func(state, image)
    },
)

inline fun CommandDsl.immutableImageEdit(
    crossinline func: ImageEdit<ImmutableImage>,
) = metadata(
    object : ImageEditCommand() {
        override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
            func(state, image.toImmutableImage()).awt()
    },
)

fun CommandDsl.generic(func: GenericCommandHandler) = metadata(func)

fun CommandDsl.metadata(func: CommandHandler) = CommandMetadata(
    help = help!!,
    name = name,
    aliases = aliases,
    availableIn = availableIn,
    handler = func,
)

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
