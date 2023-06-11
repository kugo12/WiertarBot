package pl.kvgx12.downloadapi.platforms.reddit

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.resources.serialization.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.builtins.ListSerializer
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import pl.kvgx12.downloadapi.platform.*
import pl.kvgx12.downloadapi.platform.FileExtensions.MP4
import pl.kvgx12.downloadapi.platform.Media
import pl.kvgx12.downloadapi.platforms.*
import pl.kvgx12.downloadapi.platforms.youtube.YoutubeApi.Companion.client
import pl.kvgx12.downloadapi.utils.HostPredicate
import pl.kvgx12.downloadapi.utils.base64

@ConfigurationProperties("platform.reddit")
data class RedditProperties(
    val clientId: String,
    val clientSecret: String,
)

@EnableConfigurationProperties(RedditProperties::class)
class RedditPlatform(properties: RedditProperties) : pl.kvgx12.downloadapi.platform.Platform {
    private val httpClient = redditHttpClient(properties.clientId, properties.clientSecret)

    override val name: String get() = "reddit"

    override suspend fun tryParsing(url: Url): UrlMetadata? =
        if (isRedditHost(url)) EmptyMetadata(url, MP4) else null

    override suspend fun download(
        allocator: ResourceAllocator,
        url: Url,
        metadata: UrlMetadata
    ): Media = coroutineScope {
        val body = httpClient.get("$url.json").bodyAsText()
        val redditLink = json.decodeFromString(ListSerializer(RedditListingSerializer), body)
            .first().children.first() as RedditLink

        val videoUrl = Url(redditLink.secureMedia.values.first().fallbackUrl)
        val audioUrl = URLBuilder(videoUrl).run {
            parameters.clear()
            pathSegments = pathSegments.dropLast(1) + "DASH_audio.mp4"
            build()
        }

        val video = async {
            val videoFile = allocator.allocateTempFile(videoUrl.base64(), MP4)

            client.get(videoUrl)
                .bodyAsChannel()
                .copyAndClose(videoFile.writeChannel())

            videoFile
        }

        val audio = async {
            val audioFile = allocator.allocateTempFile(audioUrl.base64(), MP4)

            client.get(audioUrl)
                .bodyAsChannel()
                .copyAndClose(audioFile.writeChannel())

            audioFile
        }

        Media.VideoAndAudio(audio.await(), video.await())
    }

    private fun isRedditHost(url: Url): Boolean = hostPredicate.test(url.host)

    companion object {
        private val hostPredicate = HostPredicate("redd.it", "reddit.com")

        private fun redditHttpClient(clientId: String, clientSecret: String) = HttpClient(CIO) {
            Auth {
                bearer {
                    realm = "reddit.com"

                    refreshTokens {
                        val token = client
                            .submitForm(
                                href(ResourcesFormat(), RedditApi.AccessToken()),
                                Parameters.build {
                                    append("grant_type", "client_credentials")
                                }
                            ) {
                                basicAuth(clientId, clientSecret)
                            }
                            .body<RedditApi.AccessToken.Response>()
                            .accessToken

                        BearerTokens(token, token)
                    }
                }
            }

            install(ContentNegotiation) {
                json(json)
            }

            defaultRequest {
                url("https://www.reddit.com")

                accept(ContentType.Application.Json)
            }
        }
    }
}
