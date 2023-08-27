package pl.kvgx12.wiertarbot.connector

import io.grpc.Server
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import pl.kvgx12.wiertarbot.connector.utils.error
import pl.kvgx12.wiertarbot.connector.utils.getLogger
import pl.kvgx12.wiertarbot.proto.Event
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class Runner(
    private val rabbitMQ: RabbitMQ,
    private val connector: Connector,
    private val server: Server,
) : CommandLineRunner {
    private val log = getLogger()

    private fun consume(event: Event) {
        runCatching {
            when {
                event.hasMessage() -> rabbitMQ.publish(event.message)
            }
        }.onFailure(log::error)
    }

    override fun run(vararg args: String) {
        server.start()

        runBlocking {
            connector.run().collect {
                launch { consume(it) }
            }
        }

        server.awaitTermination(5, TimeUnit.SECONDS)

        exitProcess(0)
    }
}
