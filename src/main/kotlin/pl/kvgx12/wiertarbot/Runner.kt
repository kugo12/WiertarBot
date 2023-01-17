package pl.kvgx12.wiertarbot

import jep.JepConfig
import jep.SharedInterpreter
import org.intellij.lang.annotations.Language
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

inline fun SharedInterpreter.execute(@Language("python") code: String) = exec(code)


@Component
class Runner: CommandLineRunner {
    override fun run(vararg args: String) {
        SharedInterpreter.setConfig(
            JepConfig().apply {
                addIncludePaths(".", ".venv/lib/python3.11/site-packages")
                redirectStdErr(System.err)
                redirectStdout(System.out)
            }
        )
        SharedInterpreter().use { interpreter ->
            interpreter.execute("import WiertarBot.__main__")
        }
    }
}