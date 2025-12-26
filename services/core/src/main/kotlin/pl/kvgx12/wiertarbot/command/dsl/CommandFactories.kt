package pl.kvgx12.wiertarbot.command.dsl

import com.sksamuel.scrimage.ImmutableImage
import pl.kvgx12.wiertarbot.command.*
import pl.kvgx12.wiertarbot.proto.FileData
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.response
import pl.kvgx12.wiertarbot.utils.toImmutableImage
import java.awt.image.BufferedImage

inline fun CommandDsl.imageEdit(
    crossinline func: ImageEdit<BufferedImage>,
) = metadata(
    object : ImageEditCommand(this@imageEdit) {
        override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
            func(state, image)
    },
)

inline fun CommandDsl.immutableImageEdit(
    crossinline func: ImageEdit<ImmutableImage>,
) = metadata(
    object : ImageEditCommand(this@immutableImageEdit) {
        override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
            func(state, image.toImmutableImage()).awt()
    },
)

fun CommandDsl.generic(func: GenericCommandHandler) = metadata(func)

fun CommandDsl.manual(func: ManualCommandHandler) = metadata(func)

fun CommandDsl.metadata(func: CommandHandler) = CommandMetadata(
    help = if (func is SpecialCommand) "" else help!!,
    name = name,
    aliases = aliases,
    availableIn = availableIn,
    handler = func,
)

inline fun CommandDsl.text(
    crossinline func: suspend (MessageEvent) -> String?,
) = generic {
    response {
        event = it
        func(it)?.let { t ->
            text = t
        }
    }
}

inline fun CommandDsl.files(
    voiceClip: Boolean = false,
    crossinline func: suspend (MessageEvent) -> List<String>,
) = generic {
    response {
        event = it
        files += it.context.upload(func(it), voiceClip)
    }
}

inline fun CommandDsl.rawFiles(
    voiceClip: Boolean = false,
    crossinline func: suspend (MessageEvent) -> List<FileData>,
) = generic {
    response {
        event = it
        files += it.context.uploadRaw(func(it), voiceClip)
    }
}

inline fun CommandDsl.file(
    voiceClip: Boolean = false,
    crossinline func: suspend (MessageEvent) -> String,
) = generic {
    response {
        event = it
        files += it.context.upload(func(it), voiceClip)
    }
}

inline fun CommandDsl.rawFile(
    voiceClip: Boolean = false,
    crossinline func: suspend (MessageEvent) -> FileData,
) = generic {
    response {
        event = it
        files += it.context.uploadRaw(func(it), voiceClip)
    }
}

fun CommandDsl.delegated() = manual { it.context.delegatedCommand(name, it) }

fun CommandDsl.special(func: SpecialCommand) = metadata(func)
