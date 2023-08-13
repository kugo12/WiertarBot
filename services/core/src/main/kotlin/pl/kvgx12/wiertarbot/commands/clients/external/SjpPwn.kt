package pl.kvgx12.wiertarbot.commands.clients.external

import it.skrape.core.htmlDocument
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import pl.kvgx12.wiertarbot.utils.appendElement

@HttpExchange("https://sjp.pwn.pl")
interface SjpPwnClient {
    @GetExchange("/slowniki/{phrase}")
    suspend fun getDefinition(@PathVariable phrase: String): String
}

class SjpPwn(private val client: SjpPwnClient) {
    suspend fun getDefinition(phrase: String): String {
        val response = client.getDefinition(
            phrase
                .lowercase()
                .replace(' ', '-'),
        )

        return buildString {
            htmlDocument(response) {
                relaxed = true
                findAll(".entry-body .ribbon-element") {
                    if (isEmpty()) {
                        append("Coś poszlo nie tak, jak nie użyłeś polskich liter, to dobry moment")
                    } else {
                        forEach {
                            appendElement(it)
                            append('\n')
                        }
                    }
                }
            }
        }.trim()
    }
}
