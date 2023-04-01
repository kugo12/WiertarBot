package pl.kvgx12.wiertarbot.command

import pl.kvgx12.wiertarbot.connector.ConnectorType
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import java.util.*

sealed interface CommandData {
    val help: String
    val name: String
    val aliases: List<String>
    val availableIn: EnumSet<ConnectorType>
}

interface Command : CommandData {
    suspend fun process(event: MessageEvent): Response?
}

fun interface SpecialCommand {
    suspend fun process(event: MessageEvent)
}
