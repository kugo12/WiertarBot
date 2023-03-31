package pl.kvgx12.wiertarbot.services

import org.springframework.scheduling.annotation.Async
import pl.kvgx12.wiertarbot.command.CommandData
import pl.kvgx12.wiertarbot.command.SpecialCommand
import pl.kvgx12.wiertarbot.utils.AllOpen
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

@AllOpen
class CommandRegistrationService(
    val permissionService: PermissionService,
    commands: List<CommandData>
) {
    val specialCommands = commands.filterIsInstance<SpecialCommand>()

    private val _aliases = mutableMapOf<String, String>()
    val aliases: Map<String, String> get() = _aliases

    private val _commands = mutableMapOf<String, CommandData>()
    val commands: Map<String, CommandData> get() = _commands


    init {
        commands.forEach {
            register(it)
        }
    }

//    @PostConstruct
//    fun postConstruct() {
//        initCommandPermissions()
//    }


    private fun register(command: CommandData) {
        _aliases.putAll(command.aliases.map { it to command.name })
        _aliases[command.name] = command.name
        _commands[command.name] = command
    }

    @Async
    fun initCommandPermissions() {
        commands.forEach { (name, _) -> permissionService.initPermissionByCommand(name) }
    }
}
