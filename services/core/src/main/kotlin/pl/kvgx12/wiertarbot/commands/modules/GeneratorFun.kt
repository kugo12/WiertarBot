package pl.kvgx12.wiertarbot.commands.modules

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import it.skrape.core.htmlDocument

object GeneratorFun {
    private const val baseUrl = "https://generatorfun.com"
    private val client = HttpClient(CIO)

    suspend fun fetchRandomImage(type: String): String {
        val response = client.get("$baseUrl/random-$type-image").bodyAsText()

        val url = htmlDocument(response) {
            findFirst(".main-template img") {
                attribute("src")
            }
        }

        return when {
            url.startsWith("http") -> url
            url.startsWith('/') -> "$baseUrl$url"
            else -> "$baseUrl/$url"
        }
    }
}
