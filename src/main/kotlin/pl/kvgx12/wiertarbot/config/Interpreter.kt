package pl.kvgx12.wiertarbot.config

import jep.JepConfig
import jep.MainInterpreter
import jep.NamingConventionClassEnquirer
import jep.SharedInterpreter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.Runner
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.repositories.FBMessageRepository
import pl.kvgx12.wiertarbot.repositories.MessageCountMilestoneRepository
import pl.kvgx12.wiertarbot.repositories.PermissionRepository
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.services.RabbitMQService
import pl.kvgx12.wiertarbot.utils.newSingleThreadDispatcher


fun BeanDefinitionDsl.interpreterBeans() {
    LoggerFactory.getLogger("interpreterBeans")
        .info("JEP_LIB_PATH=$libPath")

    bean {
        WbGlobals(
            mapOf(
                "config" to Json { encodeDefaults = true }.encodeToString(
                    WbGlobals.Config(
                        WbGlobals.WiertarBot(
                            fb = ref(),
                            sentry = WbGlobals.Sentry(python = ref())
                        )
                    )
                ),
                "permission_repository" to ref<PermissionRepository>(),
                "fb_message_repository" to ref<FBMessageRepository>(),
                "milestone_repository" to ref<MessageCountMilestoneRepository>(),
                "permission_service" to ref<PermissionService>(),
                "rabbitmq_service" to ref<RabbitMQService>()
            )
        )
    }

    bean {
        JepConfig().apply {
            addIncludePaths(".", ".venv/lib/python3.11/site-packages")
            redirectStdErr(System.err)
            redirectStdout(System.out)
            setClassLoader(Runner::class.java.classLoader)
            setClassEnquirer(NamingConventionClassEnquirer(true).apply {
                addTopLevelPackageName("pl")
            })

            SharedInterpreter.setConfig(this)
        }
    }

    bean {
        val dispatcher = newSingleThreadDispatcher("interpreter")

        runBlocking(dispatcher) {
            Interpreter(ref(), dispatcher, ref<WbGlobals>().value)
        }
    }
}

private val libPath = System.getenv("JEP_LIB_PATH")
    ?.also(MainInterpreter::setJepLibraryPath)

@JvmInline
private value class WbGlobals(val value: Map<String, Any>) {
    @Serializable
    data class Config(val wiertarbot: WiertarBot)

    @Serializable
    data class WiertarBot(val fb: FBProperties, val sentry: Sentry)

    @Serializable
    data class Sentry(val python: PythonSentryProperties)
}
