package pl.kvgx12.wiertarbot.commands

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.config.properties.DownloadApiProperties
import kotlin.time.Duration.Companion.seconds

val downloadCommand = command("download", "pobierz") {
    help(
        usage = "<url>",
        returns = "Link do pobrania pliku ważny przez 8h",
    )

    val props = dsl.ref<DownloadApiProperties>()
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        install(HttpTimeout)
    }

    text {
        val url = it.text.split(' ', limit = 2).getOrNull(1)
            ?: return@text help

        coroutineScope {
            val downloadingMessage = launch {
                delay(5.seconds)
                it.context.sendText(it, "Pobieranie filmu...")
            }

            val response = client.get(props.url) {
                parameter("url", url)

                // TODO: "async" notification through mq?
                timeout { requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS }
            }

            downloadingMessage.cancel()

            when (response.status) {
                HttpStatusCode.OK -> "Pobrano film:\n${response.body<String>()}"
                HttpStatusCode.UnprocessableEntity -> {
                    val platforms = client.get(props.platformsUrl).body<Set<String>>()
                        .joinToString(", ")
                    "Nie można pobrać filmu z podanego adresu.\nObsługiwane platformy: $platforms"
                }

                else -> "Nastąpił niespodziewany błąd"
            }
        }
    }
}
