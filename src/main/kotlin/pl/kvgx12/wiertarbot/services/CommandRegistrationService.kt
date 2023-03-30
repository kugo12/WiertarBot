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

    private val _commands = mutableMapOf<String, CommandHandler>()
    val commands: Map<String, CommandHandler> get() = _commands


    init {
        commands.forEach {
            register(it, it.toHandler())
        }
    }

//    @PostConstruct
//    fun postConstruct() {
//        initCommandPermissions()
//    }


    private fun register(data: CommandData, handler: CommandHandler) {
        _aliases.putAll(data.aliases.map { it to data.name })
        _aliases[data.name] = data.name
        _commands[data.name] = handler
    }

    @Async
    fun initCommandPermissions() {
        commands.forEach { (name, _) -> permissionService.initPermissionByCommand(name) }
    }
}
