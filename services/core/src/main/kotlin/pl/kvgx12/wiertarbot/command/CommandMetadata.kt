package pl.kvgx12.wiertarbot.command

import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.Response
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

fun interface ManualCommandHandler : CommandHandler {
    suspend fun process(event: MessageEvent)
}

fun interface SpecialCommand : CommandHandler {
    suspend fun process(event: MessageEvent)
}
