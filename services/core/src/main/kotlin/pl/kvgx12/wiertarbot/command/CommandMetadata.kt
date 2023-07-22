package pl.kvgx12.wiertarbot.command

import pl.kvgx12.wiertarbot.connector.ConnectorType
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import java.util.*

data class CommandMetadata(
    val help: String,
    val name: String,
    val aliases: List<String>,
    val availableIn: EnumSet<ConnectorType>,
    val handler: CommandHandler,
)

sealed interface CommandHandler

fun interface GenericCommandHandler : CommandHandler {
    suspend fun process(event: MessageEvent): Response?
}

fun interface SpecialCommand {
    suspend fun process(event: MessageEvent)
}
