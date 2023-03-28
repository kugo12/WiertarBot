package pl.kvgx12.wiertarbot.services

import jep.python.PyCallable
import jep.python.PyObject
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Async
import pl.kvgx12.wiertarbot.command.CommandData
import pl.kvgx12.wiertarbot.command.SpecialCommand
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.python.get
import pl.kvgx12.wiertarbot.utils.AllOpen

@AllOpen
class CommandRegistrationService(
    val interpreter: Interpreter,
    val permissionService: PermissionService,
    commands: List<CommandData>
) {
    val specialCommands = commands.filterIsInstance<SpecialCommand>()

    private val _aliases = mutableMapOf<String, String>()
    val aliases: Map<String, String> get() = _aliases

    private val _commands = mutableMapOf<String, CommandHandler>()
    val commands: Map<String, CommandHandler> get() = _commands


    init {
        runBlocking {
            registerPythonCommands()
        }

        commands.forEach {
            register(it, it.toHandler())
        }
    }

//    @PostConstruct
//    fun postConstruct() {
//        initCommandPermissions()
//    }

    private suspend fun registerPythonCommands() {
        interpreter {
            dispatcherHook(wiertarBot.init_dispatcher())
        }
    }

    private fun register(data: CommandData, handler: CommandHandler) {
        _aliases.putAll(data.aliases.map { it to data.name })
        _aliases[data.name] = data.name
        _commands[data.name] = handler
    }

    private fun dispatcherHook(dispatcher: PyObject) {
        _aliases.putAll(dispatcher.get("_alias_of"))

        dispatcher.get<Map<String, PyObject>>("_commands")
            .forEach { (name, obj) ->
                val checked = with(interpreter.inspect) {
                    when {
                        iscoroutinefunction(obj) -> CommandHandler.PyGeneric(obj as PyCallable)
                        else -> throw IllegalArgumentException(
                            "Object has invalid type, command=$name object=$obj"
                        )
                    }
                }

                _commands[name] = checked
            }

        initCommandPermissions()
    }

    @Async
    fun initCommandPermissions() {
        commands.forEach { (name, _) -> permissionService.initPermissionByCommand(name) }
    }
}
