package pl.kvgx12.wiertarbot.config

import jep.JepConfig
import jep.MainInterpreter
import jep.NamingConventionClassEnquirer
import jep.SharedInterpreter
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.Runner
import pl.kvgx12.wiertarbot.config.properties.FBProperties
import pl.kvgx12.wiertarbot.config.properties.PythonSentryProperties
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.repositories.FBMessageRepository
import pl.kvgx12.wiertarbot.repositories.MessageCountMilestoneRepository
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.services.RabbitMQService
import pl.kvgx12.wiertarbot.utils.newSingleThreadDispatcher

fun BeanDefinitionDsl.interpreterBeans() {
    LoggerFactory.getLogger("interpreterBeans")
        .info("JEP_LIB_PATH=$libPath")

    bean {
        WbGlobals(
            mapOf(
                "fb_message_repository" to ref<FBMessageRepository>(),
                "milestone_repository" to ref<MessageCountMilestoneRepository>(),
                "permission_service" to ref<PermissionService>(),
                "rabbitmq_service" to ref<RabbitMQService>(),
                "fb_properties" to ref<FBProperties>(),
                "sentry_properties" to provider<PythonSentryProperties>().firstOrNull(),
            ),
        )
    }

    bean {
        JepConfig().apply {
            addIncludePaths(".", ".venv/lib/python3.11/site-packages")
            redirectStdErr(System.err)
            redirectStdout(System.out)
            setClassLoader(Runner::class.java.classLoader)
            setClassEnquirer(
                NamingConventionClassEnquirer(true).apply {
                    addTopLevelPackageName("pl")
                },
            )

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
private value class WbGlobals(val value: Map<String, Any?>)
