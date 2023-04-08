package pl.kvgx12.wiertarbot.commands.modules

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.skrape.core.htmlDocument

object Fantano {
    private val client = HttpClient(CIO)

    val baseUrl = Url("https://www.theneedledrop.com/")
    val mbdtfUrl = Url("https://www.theneedledrop.com/articles/2020/1/kanye-west-my-beautiful-dark-twisted-fantasy")

    data class Review(val title: String, val review: String, val rate: String?)

    suspend fun getRate(term: String) = getRateFromUrl(findReview(term))

    private suspend fun getRateFromUrl(url: Url): Review {
        val body = client.get(url).bodyAsText()

        return htmlDocument(body) {
            Review(
                findFirst(".entry-title") { text },
                findFirst("p") { text },
                findAll(".entry-tags > *") { firstOrNull { it.text.contains("/10") }?.text },
            )
        }
    }

    private suspend fun findReview(term: String): Url {
        val url = URLBuilder(baseUrl)
            .appendPathSegments("search")
            .build()

        val body = client.get(url) {
            parameter("q", term)
        }.bodyAsText()

        return htmlDocument(body) {
            relaxed = true

            ".search-result" {
                withAttributeKey = "data-url"

                findAll {
                    firstOrNull()
                        ?.attribute("data-url")
                        ?.let { URLBuilder(baseUrl).appendPathSegments(it).build() }
                }
            }
        } ?: mbdtfUrl
    }
}
