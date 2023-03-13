package pl.kvgx12.wiertarbot.config

import pl.kvgx12.wiertarbot.events.MessageEvent

fun interface SpecialCommand: suspend (MessageEvent) -> Unit

class SpecialCommandsConfiguration {
    private val _commands = mutableListOf<SpecialCommand>()

    val commands: List<SpecialCommand>
        get() = _commands

    fun new(vararg func: SpecialCommand) = _commands.addAll(func)
}