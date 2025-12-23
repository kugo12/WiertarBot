package pl.kvgx12.telegram

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.*
import io.ktor.http.URLProtocol.Companion.HTTPS
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class TelegramClient(
    token: String,
) {
    val basePath = TelegramApi(token)
    val filePath = TelegramFile(token)
    private val client = createClient()


    companion object {
        private fun createClient() = HttpClient(CIO) {
            install(Resources)

            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }

            ContentEncoding {
                gzip()
                deflate()
            }


            Logging {
                level = LogLevel.ALL
            }

            defaultRequest {
                host = "api.telegram.org"
                url { protocol = HTTPS }
            }
        }
    }
}
