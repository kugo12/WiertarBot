package pl.kvgx12.downloadapi.platforms.tiktok

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.skrape.core.htmlDocument
import it.skrape.selects.html5.script
import kotlinx.serialization.json.Json
import pl.kvgx12.downloadapi.platform.*
import pl.kvgx12.downloadapi.platform.FileExtensions.MP4
import pl.kvgx12.downloadapi.utils.FindUrlWithKeyTraversal
import pl.kvgx12.downloadapi.utils.HostPredicate

class TikTokPlatform : Platform {
    override val name: String get() = "tiktok"

    override suspend fun tryParsing(url: Url): UrlMetadata? {
        if (!isTikTokUrl(url)) return null

        val videoId = videoIdFrom(url)
            ?: videoIdFrom(Platform.followRedirects(url))

        return videoId?.let {
            VideoIdUrlMetadata(url, it)
        }
    }

    override suspend fun download(
        allocator: ResourceAllocator,
        url: Url,
        metadata: UrlMetadata,
    ): Media {
        metadata as VideoIdUrlMetadata

        val body = Platform.client.get(url).bodyAsText()
        val jsonState = htmlDocument(body) {
            script {
                withId = "SIGI_STATE"

                findFirst { html }
            }
        }

        val mediaUrl = traversal.traverse(Json.decodeFromString(jsonState)).first()
        val videoFile = allocator.allocateTempFile(metadata.videoId, MP4)

        Platform.getFile(mediaUrl, videoFile) {
            header(HttpHeaders.Referrer, "https://www.tiktok.com/")
        }

        return Media.Video(videoFile)
    }

    private fun isTikTokUrl(url: Url): Boolean = hostPredicate.test(url.host)

    private fun videoIdFrom(url: Url): String? {
        val segments = url.pathSegments

        if (segments.size >= 2 && segments[segments.lastIndex - 1] == "video") {
            return segments.last()
        }

        return null
    }

    companion object {
        private val hostPredicate = HostPredicate("tiktok.com")
        private val traversal = FindUrlWithKeyTraversal("playAddr")
    }
}
