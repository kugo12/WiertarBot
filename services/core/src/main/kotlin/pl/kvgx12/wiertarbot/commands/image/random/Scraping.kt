package pl.kvgx12.wiertarbot.commands.image.random

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.command.dsl.file
import pl.kvgx12.wiertarbot.commands.modules.GeneratorFun

val randomImageScrapingCommands = commands {
    command("frog", "zabka", "żabka", "zaba", "żaba") {
        help(returns = "zdjęcie z żabką")

        file { GeneratorFun.fetchRandomImage("frog") }
    }

    command("jez", "jeż", "hedgehog") {
        help(returns = "zdjęcie z jeżykiem")

        file { GeneratorFun.fetchRandomImage("hedgehog") }
    }

    command("cat", "catto", "kot") {
        help(returns = "zdjęcie z kotem")

        file { GeneratorFun.fetchRandomImage("cat") }
    }
}

private val client = HttpClient(CIO)
