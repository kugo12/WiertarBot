package pl.kvgx12.wiertarbot.command.dsl

import org.springframework.beans.factory.BeanRegistrarDsl
import pl.kvgx12.wiertarbot.command.CommandMetadata

fun commands(func: BeanRegistrarDsl.() -> Unit) = func

inline fun command(
    name: String,
    vararg aliases: String,
    crossinline func: CommandDsl.() -> CommandMetadata,
): BeanRegistrarDsl.() -> Unit = {
    command(name, *aliases, func = func)
}

inline fun BeanRegistrarDsl.command(
    name: String,
    vararg aliases: String,
    crossinline func: CommandDsl.() -> CommandMetadata,
) = registerBean(name) {
    CommandDsl(this, name = name, aliases = aliases.toList())
        .let(func)
}

fun specialCommandName(name: String) = "special-$name"
