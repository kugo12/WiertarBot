package pl.kvgx12.wiertarbot

import io.ktor.util.logging.*
import jep.Jep
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import pl.kvgx12.wiertarbot.connector.Connector
import pl.kvgx12.wiertarbot.connectors.FBConnector
import pl.kvgx12.wiertarbot.connectors.TelegramConnector
import pl.kvgx12.wiertarbot.events.Event
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.services.CommandService
import pl.kvgx12.wiertarbot.utils.getLogger
import kotlin.system.exitProcess

inline fun Jep.execute(@Language("python") code: String) = exec(code)


@Component
@Profile("!test")
class Runner(
    private val commandService: CommandService,
    private val connectors: List<Connector>,
) : CommandLineRunner {
    private val log = getLogger()

    init {
        require(connectors.isNotEmpty()) { "No connectors available" }
    }

    private suspend fun collector(event: Event) {
        try {
            when (event) {
                is MessageEvent -> commandService.dispatch(event)
            }
        } catch (e: Throwable) {
            log.error(e)
        }
    }

    override fun run(vararg args: String) {
        runBlocking {
            connectors
                .map { it.run().onEach(::collector).launchIn(this) }
                .forEach { it.join() }

            exitProcess(0)
        }
    }
}