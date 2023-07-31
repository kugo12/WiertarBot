package pl.kvgx12.wiertarbot.commands

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.text
import pl.kvgx12.wiertarbot.config.properties.WeatherProperties

val weatherCommand = command("pogoda", "weather") {
    help(
        usage = "<miasto>",
        returns = "pogoda w danym mieście",
    )

    val props = dsl.ref<WeatherProperties>()
    val client = HttpClient(CIO)

    text {
        val city = it.text.split(' ', limit = 2)
            .getOrNull(1)
            ?.takeIf { it.isNotBlank() }
            ?: return@text help

        val response = client.get {
            url {
                takeFrom(props.url)
                parameters.append("city", city)
            }
        }

        when (response.status) {
            HttpStatusCode.NotFound -> "Nie znaleziono podanego miasta"
            HttpStatusCode.OK -> response.bodyAsText()
            else -> "Wystąpił niespodziewany błąd"
        }
    }
}
