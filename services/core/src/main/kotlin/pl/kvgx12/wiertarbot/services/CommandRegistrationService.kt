package pl.kvgx12.wiertarbot.services

import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Async
import pl.kvgx12.wiertarbot.command.CommandMetadata
import pl.kvgx12.wiertarbot.command.SpecialCommand
import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.utils.AllOpen
import kotlin.collections.component1
import kotlin.collections.component2

@AllOpen
class CommandRegistrationService(
    val permissionService: PermissionService,
    commands: List<CommandMetadata>,
) {
    val specialCommands = commands
        .map { it.handler }
        .filterIsInstance<SpecialCommand>()

    private val commands: Map<String, CommandMetadata> = commands
        .filter { it.handler !is SpecialCommand }
        .associateBy { it.name }

    val aliases: Map<String, String> = commands
        .filter { it.handler !is SpecialCommand }
        .flatMap { command ->
            command.aliases.map {
                it to command.name
            } + (command.name to command.name)
        }.toMap()

    val commandsByConnector: Map<ConnectorType, Map<String, CommandMetadata>> =
        ConnectorType.entries.associateWith { type ->
            commands
                .filter { it.handler !is SpecialCommand && it.availableIn.contains(type) }
                .associateBy { it.name }
        }

    @PostConstruct
    fun postConstruct() {
        initCommandPermissions()
    }

    @Async
    fun initCommandPermissions() {
        runBlocking {
            commands.forEach { (name, _) -> permissionService.initPermissionByCommand(name) }
        }
    }
}
