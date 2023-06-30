package pl.kvgx12.downloadapi.platforms.streamable

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.skrape.core.htmlDocument
import pl.kvgx12.downloadapi.platform.*
import pl.kvgx12.downloadapi.utils.HostPredicate
import pl.kvgx12.downloadapi.utils.lastPathSegment

class StreamablePlatform : Platform {
    override val name = "streamable"

    override suspend fun tryParsing(url: Url): UrlMetadata? {
        if (!isValidUrl(url)) {
            return null
        }

        return VideoIdUrlMetadata(url, url.lastPathSegment)
    }

    override suspend fun download(allocator: ResourceAllocator, url: Url, metadata: UrlMetadata): Media {
        metadata as VideoIdUrlMetadata

        val response = Platform.client.get(url).bodyAsText()

        val videoUrl = htmlDocument(response) {
            findFirst("video") {
                attribute("src")
            }
        }

        val file = allocator.allocateTempFile(metadata.videoId, FileExtensions.MP4)
        Platform.getFile(Url(videoUrl), file)

        return Media.Video(file)
    }

    private fun isValidUrl(url: Url) =
        hostPredicate.test(url.host) &&
            url.pathSegments.size == 1

    companion object {
        private val hostPredicate = HostPredicate("streamable.com")
    }
}
