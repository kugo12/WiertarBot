package pl.kvgx12.wiertarbot.command.dsl

import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.command.CommandMetadata
import pl.kvgx12.wiertarbot.command.SpecialCommand

fun commands(func: BeanDefinitionDsl.() -> Unit) = func

inline fun command(
    name: String,
    vararg aliases: String,
    crossinline func: CommandDsl.() -> CommandMetadata,
): BeanDefinitionDsl.() -> Unit = {
    command(name, *aliases, func = func)
}

inline fun BeanDefinitionDsl.command(
    name: String,
    vararg aliases: String,
    crossinline func: CommandDsl.() -> CommandMetadata,
) = bean(name) {
    CommandDsl(this, name = name, aliases = aliases.toList())
        .let(func)
}

fun specialCommandName(name: String) = "special-$name"
