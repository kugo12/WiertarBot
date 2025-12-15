package pl.kvgx12.wiertarbot.commands

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.web.reactive.function.client.WebClientResponseException
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.text
import pl.kvgx12.wiertarbot.commands.clients.internal.DownloadClient
import kotlin.time.Duration.Companion.seconds

class DownloadCommandRegistrar : BeanRegistrarDsl({
    if (env.getProperty("wiertarbot.download-api.url") != null) {
        command("download", "pobierz") {
            help(
                usage = "<url>",
                returns = "Link do pobrania pliku ważny przez 8h",
            )

            val client = dsl.bean<DownloadClient>()

            text {
                val url = it.text.split(' ', limit = 2).getOrNull(1)
                    ?: return@text help

                coroutineScope {
                    val downloadingMessage = launch {
                        delay(5.seconds)
                        it.context.sendText(it, "Pobieranie filmu...")
                    }

                    // TODO: "async" notification through mq?
                    try {
                        val downloadPublicUrl = try {
                            client.download(url)
                        } finally {
                            downloadingMessage.cancel()
                        }

                        "Pobrano film:\n${downloadPublicUrl}"
                    } catch (_: WebClientResponseException.UnprocessableEntity) {
                        val platforms = client.supportedPlatforms()
                            .joinToString(", ")

                        "Nie można pobrać filmu z podanego adresu.\nObsługiwane platformy: $platforms"
                    } catch (e: Exception) {
                        "Nastąpił niespodziewany błąd"
                    }
                }
            }
        }
    }
})
