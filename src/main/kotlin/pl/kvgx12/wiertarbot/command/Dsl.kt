package pl.kvgx12.wiertarbot.command

import com.sksamuel.scrimage.ImmutableImage
import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.connector.FileData
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.utils.toImmutableImage
import java.awt.image.BufferedImage

class CommandDsl(
    val dsl: BeanDefinitionDsl.BeanSupplierContext,
    val name: String,
    val aliases: List<String> = emptyList(),
) {
    var help: String? = null

    inline fun imageEdit(
        crossinline func: ImageEdit<BufferedImage>
    ) = object : ImageEditCommand(help!!, name, aliases) {
        override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
            func(state, image)
    }

    inline fun immutableImageEdit(
        crossinline func: ImageEdit<ImmutableImage>
    ) = object : ImageEditCommand(help!!, name, aliases) {
        override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
            func(state, image.toImmutableImage()).awt()
    }

    inline fun generic(
        crossinline func: suspend (MessageEvent) -> Response?,
    ): CommandData {
        val help = help!!
        val name = name
        val aliases = aliases

        return object : Command {
            override val help: String get() = help
            override val name: String get() = name
            override val aliases: List<String> get() = aliases

            override suspend fun process(event: MessageEvent): Response? = func(event)
        }
    }

    inline fun text(
        crossinline func: suspend (MessageEvent) -> String?
    ) = generic { Response(it, text = func(it)) }

    inline fun files(
        voiceClip: Boolean = false,
        crossinline func: suspend (MessageEvent) -> List<String>
    ) = generic { Response(it, files = it.context.upload(func(it), voiceClip)) }

    inline fun rawFiles(
        voiceClip: Boolean = false,
        crossinline func: suspend (MessageEvent) -> List<FileData>
    ) = generic { Response(it, files = it.context.uploadRaw(func(it), voiceClip)) }


    inline fun help(eval: HelpEval) {
        help = eval(
            HelpEvaluationContext.from(this),
            StringBuilder()
        ).toString()
    }

    inline fun help(usage: String = "", returns: String? = null, info: String? = null) {
        help { builder ->
            builder.apply {
                usage(usage)
                returns?.let { returns(it) }
                info?.let { info(it) }
            }
        }
    }
}

inline fun commands(noinline func: BeanDefinitionDsl.() -> Unit) = func

inline fun command(
    name: String,
    vararg aliases: String,
    crossinline func: CommandDsl.() -> CommandData
): BeanDefinitionDsl.() -> Unit = {
    command(name, *aliases, func = func)
}

inline fun BeanDefinitionDsl.command(
    name: String,
    vararg aliases: String,
    crossinline func: CommandDsl.() -> CommandData
) =
    bean {
        CommandDsl(this, name = name, aliases = aliases.toList())
            .let(func)
    }

inline fun BeanDefinitionDsl.specialCommandWithContext(crossinline func: BeanDefinitionDsl.BeanSupplierContext.() -> SpecialCommand) =
    bean { func() }

inline fun BeanDefinitionDsl.specialCommand(func: SpecialCommand) =
    bean { func }
