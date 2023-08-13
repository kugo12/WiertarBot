package pl.kvgx12.wiertarbot.commands.clients.external

import it.skrape.core.htmlDocument
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

private const val baseUrl = "https://generatorfun.com"

@HttpExchange(baseUrl)
interface GeneratorFunClient {
    @GetExchange("/random-{type}-image")
    suspend fun randomImage(@PathVariable type: String): String
}

class GeneratorFun(private val client: GeneratorFunClient) {
    suspend fun randomImage(type: String): String {
        val response = client.randomImage(type)

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
