package pl.kvgx12.wiertarbot

import jep.Jep
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import pl.kvgx12.wiertarbot.connectors.FBConnector
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.services.CommandService
import kotlin.system.exitProcess

inline fun Jep.execute(@Language("python") code: String) = exec(code)


@Component
class Runner(
    private val fbConnector: FBConnector,
    private val commandService: CommandService,
) : CommandLineRunner {
    override fun run(vararg args: String) {
        runBlocking {
            fbConnector.run().collect {
                when (it) {
                    is MessageEvent -> commandService.dispatch(it)
                }
            }

            exitProcess(0)
        }
    }
}