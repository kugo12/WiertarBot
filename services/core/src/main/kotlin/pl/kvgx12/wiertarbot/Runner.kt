package pl.kvgx12.wiertarbot

import io.ktor.util.logging.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import pl.kvgx12.wiertarbot.connector.Connector
import pl.kvgx12.wiertarbot.events.Event
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.services.CommandService
import pl.kvgx12.wiertarbot.utils.getLogger
import kotlin.system.exitProcess

class Runner(
    private val commandService: CommandService,
    private val connectors: List<Connector>,
) : CommandLineRunner {
    private val log = getLogger()

    init {
        require(connectors.isNotEmpty()) { "No connectors available" }
    }

    private suspend fun consume(event: Event) {
        runCatching {
            when (event) {
                is MessageEvent -> commandService.dispatch(event)
            }
        }.onFailure(log::error)
    }

    override fun run(vararg args: String) {
        runBlocking {
            connectors.forEach {
                it.run().onEach {
                    launch { consume(it) }
                }.launchIn(this)
            }
        }

        exitProcess(0)
    }
}
