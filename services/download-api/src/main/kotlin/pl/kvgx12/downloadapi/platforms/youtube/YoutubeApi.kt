package pl.kvgx12.downloadapi.platforms.youtube

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Resource("/youtubei/v1")
class YoutubeApi {
    companion object {
        const val baseUrl = "https://www.youtube-nocookie.com"
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            install(Resources)

            defaultRequest {
                url(baseUrl)

                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
            }
        }
    }

    @Resource("/player")
    data class Player(val parent: YoutubeApi = YoutubeApi(), val prettyPrint: Boolean = false) {
        @Serializable
        data class Request(val videoId: String, val context: Context) {
            @Serializable
            data class Context(val client: Client)

            @Serializable
            data class Client(val clientName: String, val clientVersion: String)
        }

        @Serializable
        data class Response(val streamingData: StreamingData) {
            @Serializable
            data class StreamingData(
                val formats: List<Format>,
                val adaptiveFormats: List<Format>
            )

            @Serializable
            data class Format(
                val width: Int? = null,
                val height: Int? = null,
                val url: String,
                val fps: Int? = null,
                val qualityLabel: String? = null,
                val audioSampleRate: String? = null
            )
        }

        companion object {
            suspend fun request(
                videoId: String,
                clientName: String,
                clientVersion: String,
                client: HttpClient = YoutubeApi.client
            ) = runCatching {
                client.post(Player()) {
                    setBody(Request(videoId, Request.Context(Request.Client(clientName, clientVersion))))
                }.body<Response>()
            }
        }
    }
}
