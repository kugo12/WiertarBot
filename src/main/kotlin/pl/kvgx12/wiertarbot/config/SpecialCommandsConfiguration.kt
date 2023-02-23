package pl.kvgx12.wiertarbot.config

import org.springframework.context.annotation.Configuration
import pl.kvgx12.wiertarbot.events.MessageEvent

typealias SpecialCommand = suspend (MessageEvent) -> Unit

@Configuration
class SpecialCommandsConfiguration {
    private val _commands = mutableListOf<SpecialCommand>()

    val commands: List<SpecialCommand>
        get() = _commands

    fun new(vararg func: SpecialCommand) = _commands.addAll(func)
}