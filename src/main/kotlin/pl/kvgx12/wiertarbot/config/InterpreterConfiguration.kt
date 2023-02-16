package pl.kvgx12.wiertarbot.config

import jep.JepConfig
import jep.MainInterpreter
import jep.SharedInterpreter
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.kvgx12.wiertarbot.Runner
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.repositories.FBMessageRepository
import pl.kvgx12.wiertarbot.repositories.MessageCountMilestoneRepository
import pl.kvgx12.wiertarbot.repositories.PermissionRepository
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.services.RabbitMQService
import pl.kvgx12.wiertarbot.utils.newSingleThreadDispatcher
import java.util.concurrent.Executors


@Configuration
class InterpreterConfiguration {
    companion object {
        init {
            MainInterpreter.setJepLibraryPath(
                "/Users/kvgx12/Desktop/projekty/moje/WiertarBot/.venv/lib/python3.11/site-packages/jep/libjep.jnilib"
            )
        }
    }

    @Bean
    fun jepConfig() = JepConfig().apply {
        addIncludePaths(".", ".venv/lib/python3.11/site-packages")
        redirectStdErr(System.err)
        redirectStdout(System.out)
        setClassLoader(Runner::class.java.classLoader)

        SharedInterpreter.setConfig(this)
    }

    @Bean
    fun interpreter(
        config: JepConfig,
        globals: WbGlobals,
    ): Interpreter {
        val dispatcher = newSingleThreadDispatcher("interpreter")

        return runBlocking(dispatcher) {
            Interpreter(config, dispatcher, globals.value)
        }
    }

    @Bean
    fun wbGlobals(
        props: WiertarbotProperties,
        permissionRepository: PermissionRepository,
        fbMessageRepository: FBMessageRepository,
        milestoneRepository: MessageCountMilestoneRepository,
        permissionService: PermissionService,
        rabbitMQService: RabbitMQService,
    ) = WbGlobals(
        mapOf(
            "config" to Json { encodeDefaults = true }.encodeToString(WbGlobals.Config(props)),
            "permission_repository" to permissionRepository,
            "fb_message_repository" to fbMessageRepository,
            "milestone_repository" to milestoneRepository,
            "permission_service" to permissionService,
            "rabbitmq_service" to rabbitMQService
        )
    )

    @JvmInline
    value class WbGlobals(val value: Map<String, Any>) {
        @Serializable
        data class Config(val wiertarbot: WiertarbotProperties)
    }
}