package pl.kvgx12.wiertarbot.command.dsl

import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.command.CommandData
import pl.kvgx12.wiertarbot.command.SpecialCommand

fun commands(func: BeanDefinitionDsl.() -> Unit) = func

inline fun command(
    name: String,
    vararg aliases: String,
    crossinline func: CommandDsl.() -> CommandData,
): BeanDefinitionDsl.() -> Unit = {
    command(name, *aliases, func = func)
}

inline fun BeanDefinitionDsl.command(
    name: String,
    vararg aliases: String,
    crossinline func: CommandDsl.() -> CommandData,
) = bean(name) {
    CommandDsl(this, name = name, aliases = aliases.toList())
        .let(func)
}

inline fun BeanDefinitionDsl.specialCommandWithContext(
    name: String,
    crossinline func: BeanDefinitionDsl.BeanSupplierContext.() -> SpecialCommand,
) = bean(specialCommandName(name)) { func() }

fun BeanDefinitionDsl.specialCommand(name: String, func: SpecialCommand) =
    specialCommandWithContext(name) { func }

fun specialCommandName(name: String) = "special-$name"
