package pl.kvgx12.wiertarbot.commands.clients.external

import it.skrape.core.htmlDocument
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import pl.kvgx12.wiertarbot.utils.appendElement

@HttpExchange("https://codziennyhumor.pl")
interface SucharClient {
    @GetExchange("/")
    suspend fun get(@RequestParam("filter-by") filterBy: String): String
}

class Suchar(private val client: SucharClient) {
    suspend fun random(): String {
        val response = client.get("random")

        return buildString {
            htmlDocument(response) {
                ".entry-body" {
                    appendLine(findFirst(".entry-header").text)
                    appendElement(findFirst(".entry-content p"))
                }
            }
        }
    }
}
