package pl.kvgx12.wiertarbot.services

import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Async
import pl.kvgx12.wiertarbot.command.CommandData
import pl.kvgx12.wiertarbot.connector.ConnectorType
import pl.kvgx12.wiertarbot.utils.AllOpen
import kotlin.collections.component1
import kotlin.collections.component2

@AllOpen
class CommandRegistrationService(
    val permissionService: PermissionService,
    commands: List<CommandData>,
) {
    private val commands: Map<String, CommandData> = commands.associateBy { it.name }

    val aliases: Map<String, String> = commands.flatMap { command ->
        command.aliases.map {
            it to command.name
        } + (command.name to command.name)
    }.toMap()

    val commandsByConnector: Map<ConnectorType, Map<String, CommandData>> =
        ConnectorType.all().associateWith { type ->
            commands
                .filter { it.availableIn.contains(type) }
                .associateBy { it.name }
        }

    @PostConstruct
    fun postConstruct() {
        initCommandPermissions()
    }

    @Async
    fun initCommandPermissions() {
        commands.forEach { (name, _) -> permissionService.initPermissionByCommand(name) }
    }
}
