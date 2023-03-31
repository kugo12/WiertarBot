package pl.kvgx12.wiertarbot.command

import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response

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
