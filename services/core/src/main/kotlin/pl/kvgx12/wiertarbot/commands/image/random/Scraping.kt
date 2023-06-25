package pl.kvgx12.wiertarbot.commands.image.random

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import it.skrape.core.htmlDocument
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.commands.modules.GeneratorFun

val randomImageScrapingCommands = commands {
    command("suchar") {
        help(returns = "zdjęcie z sucharem")

        files { event ->
            val response = client.get("https://www.suchary.com/random.html").bodyAsText()

            val url = htmlDocument(response) {
                findFirst(".file-container a img") {
                    attribute("src")
                }
            }

            listOf(url)
        }
    }

    command("frog", "zabka", "żabka", "zaba", "żaba") {
        help(returns = "zdjęcie z żabką")

        files {
            listOf(GeneratorFun.fetchRandomImage("frog"))
        }
    }

    command("jez", "jeż", "hedgehog") {
        help(returns = "zdjęcie z jeżykiem")

        files {
            listOf(GeneratorFun.fetchRandomImage("hedgehog"))
        }
    }
}

private val client = HttpClient(CIO)
