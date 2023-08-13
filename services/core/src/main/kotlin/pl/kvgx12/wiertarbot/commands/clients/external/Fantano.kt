package pl.kvgx12.wiertarbot.commands.clients.external

import it.skrape.core.htmlDocument
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange("https://www.theneedledrop.com")
interface FantanoClient {
    @GetExchange("/search")
    suspend fun search(@RequestParam("q") query: String): String

    @GetExchange("")
    suspend fun mbdtf(): String

    @GetExchange("{path}")
    suspend fun get(@PathVariable("path") path: String): String
}

class Fantano(private val client: FantanoClient) {
    data class Review(val title: String, val review: String, val rate: String?)

    suspend fun getRate(term: String) = getRateFromUrl(findReview(term))

    private suspend fun getRateFromUrl(path: String): Review {
        return htmlDocument(client.get(path)) {
            Review(
                findFirst(".entry-title") { text },
                findFirst("p") { text },
                findAll(".entry-tags > *") { firstOrNull { it.text.contains("/10") }?.text },
            )
        }
    }

    private suspend fun findReview(term: String): String {
        val body = client.search(term)

        return htmlDocument(body) {
            relaxed = true

            ".search-result" {
                withAttributeKey = "data-url"

                findAll {
                    firstOrNull()?.attribute("data-url")
                }
            }
        } ?: "/articles/2020/1/kanye-west-my-beautiful-dark-twisted-fantasy"
    }
}
