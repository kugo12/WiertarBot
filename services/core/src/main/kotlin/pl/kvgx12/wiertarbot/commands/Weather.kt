package pl.kvgx12.wiertarbot.commands

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.web.reactive.function.client.WebClientResponseException
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.text
import pl.kvgx12.wiertarbot.commands.clients.internal.WeatherClient

class WeatherCommandRegistrar : BeanRegistrarDsl({
    if (env.getProperty("wiertarbot.weather.url") != null) {
        command("pogoda", "weather") {
            help(
                usage = "<miasto>",
                returns = "pogoda w danym mieście",
            )

            val client = dsl.bean<WeatherClient>()

            text {
                val city = it.text.split(' ', limit = 2)
                    .getOrNull(1)
                    ?.takeIf { it.isNotBlank() }
                    ?: return@text help

                try {
                    client.weather(city)
                } catch (_: WebClientResponseException.NotFound) {
                    "Nie znaleziono podanego miasta"
                } catch (e: Exception) {
                    "Wystąpił niespodziewany błąd"
                }
            }
        }
    }
})
