package pl.kvgx12.wiertarbot.command

import com.sksamuel.scrimage.ImmutableImage
import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.utils.toImmutableImage
import java.awt.image.BufferedImage

sealed interface CommandData {
    val help: String
    val name: String
    val aliases: List<String>
}

interface Command : CommandData {
    suspend fun process(event: MessageEvent): Response?
}

fun interface SpecialCommand {
    suspend fun process(event: MessageEvent)
}

class CommandDsl(
    val dsl: BeanDefinitionDsl.BeanSupplierContext,
) {
    var help: String? = null
    var name: String? = null
    var aliases: List<String> = emptyList()

    inline fun imageEdit(
        crossinline func: ImageEdit<BufferedImage>
    ) = object : ImageEditCommand(help!!, name!!, aliases) {
        override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
            func(state, image)
    }

    inline fun immutableImageEdit(
        crossinline func: ImageEdit<ImmutableImage>
    ) = object : ImageEditCommand(help!!, name!!, aliases) {
        override suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage =
            func(state, image.toImmutableImage()).awt()
    }

    inline fun generic(
        crossinline func: suspend (MessageEvent) -> Response?,
    ): CommandData {
        val help = help!!
        val name = name!!
        val aliases = aliases

        return object : Command {
            override val help: String get() = help
            override val name: String get() = name
            override val aliases: List<String> get() = aliases

            override suspend fun process(event: MessageEvent): Response? = func(event)
        }
    }

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

inline fun command(crossinline func: CommandDsl.() -> CommandData): BeanDefinitionDsl.() -> Unit = {
    command(func)
}

inline fun BeanDefinitionDsl.command(crossinline func: CommandDsl.() -> CommandData) =
    bean {
        CommandDsl(this).let(func)
    }

inline fun BeanDefinitionDsl.specialCommandWithContext(crossinline func: BeanDefinitionDsl.BeanSupplierContext.() -> SpecialCommand) =
    bean { func() }

inline fun BeanDefinitionDsl.specialCommand(func: SpecialCommand) =
    bean { func }
