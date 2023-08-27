package pl.kvgx12.wiertarbot.commands.clients.external

import it.skrape.core.htmlDocument
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import pl.kvgx12.wiertarbot.utils.appendElement

@HttpExchange("https://www.miejski.pl")
interface MiejskiClient {
    @GetExchange("/slowo-{phrase}")
    suspend fun getDefinition(@PathVariable phrase: String): String
}

class Miejski(private val client: MiejskiClient) {
    suspend fun getDefinition(phrase: String): String {
        val response = try {
            client.getDefinition(
                phrase
                    .lowercase()
                    .replace(' ', '+'),
            )
        } catch (_: WebClientResponseException.NotFound) {
            return "Nie znaleziono podanego słowa"
        }

        return buildString {
            append(phrase)
            append('\n')

            htmlDocument(response) {
                findFirst("main p") {
                    append("Definicja:\n")
                    appendElement(this)
                }

                relaxed = true
                findAll("main blockquote").firstOrNull()?.apply {
                    if (text.isNotEmpty()) {
                        append("\n\nPrzyklad/y:\n")
                        appendElement(this)
                    }
                }
            }
        }.trim()
    }
}
