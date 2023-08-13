package pl.kvgx12.wiertarbot.connector


import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.connector.DelegatedCommandRequest

fun interface DelegatedCommand {
    suspend fun process(event: MessageEvent)

    companion object {
        const val PREFIX = "dc-"

        fun name(name: String) = "$PREFIX$name"
    }
}

class DelegatedCommandInvoker(commands: Map<String, DelegatedCommand>) {
    private val commands = commands.mapKeys {
        it.key.removePrefix(DelegatedCommand.PREFIX)
    }

    suspend operator fun invoke(request: DelegatedCommandRequest) {
        require(request.command in commands) {
            "Unknown command: ${request.command}"
        }

        commands[request.command]!!.process(request.event)
    }
}
