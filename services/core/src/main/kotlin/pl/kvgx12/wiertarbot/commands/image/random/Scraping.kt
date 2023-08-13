package pl.kvgx12.wiertarbot.commands.image.random

import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.command.dsl.file
import pl.kvgx12.wiertarbot.commands.clients.external.GeneratorFun

val randomImageScrapingCommands = commands {
    command("frog", "zabka", "żabka", "zaba", "żaba") {
        help(returns = "zdjęcie z żabką")

        val client = dsl.ref<GeneratorFun>()

        file { client.randomImage("frog") }
    }

    command("jez", "jeż", "hedgehog") {
        help(returns = "zdjęcie z jeżykiem")

        val client = dsl.ref<GeneratorFun>()

        file { client.randomImage("hedgehog") }
    }

    command("cat", "catto", "kot") {
        help(returns = "zdjęcie z kotem")

        val client = dsl.ref<GeneratorFun>()

        file { client.randomImage("cat") }
    }
}
