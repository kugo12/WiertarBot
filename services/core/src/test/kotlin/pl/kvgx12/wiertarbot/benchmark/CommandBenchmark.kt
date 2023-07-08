package pl.kvgx12.wiertarbot.benchmark

import io.mockk.mockk
import kotlinx.benchmark.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.OperationsPerInvocation
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.services.CommandService

@State(Scope.Benchmark)
class CommandBenchmark : AbstractSpringBenchmark() {
    var commandService: CommandService? = null
    var event: MessageEvent? = null

    override fun ConfigurableApplicationContext.setup() {
        commandService = getBean()
        event = MessageEvent(mockk(), "!czas", "", "", 0, listOf(), "", null, listOf())
    }

    override fun tearDown() {
        commandService = null
        event = null
    }

    @Benchmark
    @OperationsPerInvocation(100)
    fun dispatchAsync100() {
        runBlocking {
            (1..100)
                .map {
                    async {
                        commandService!!.dispatch(event!!)
                    }
                }.awaitAll()
        }
    }

    @Benchmark
    fun dispatchSingle() {
        runBlocking {
            commandService!!.dispatch(event!!)
        }
    }
}
