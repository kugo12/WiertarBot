package pl.kvgx12.downloadapi.platforms.instagram

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import pl.kvgx12.downloadapi.platform.*
import pl.kvgx12.downloadapi.platform.FileExtensions.MP4
import pl.kvgx12.downloadapi.utils.FindUrlWithKeyTraversal
import pl.kvgx12.downloadapi.utils.HostPredicate
import pl.kvgx12.downloadapi.utils.Url
import pl.kvgx12.downloadapi.utils.getSegmentAfter

class InstagramPlatform : Platform {
    override val name: String get() = "instagram"

    override suspend fun tryParsing(url: Url): UrlMetadata? {
        if (!isInstagramHost(url)) return null

        return url.getSegmentAfter("reel")?.let {
            VideoIdUrlMetadata(url, it)
        }
    }

    override suspend fun download(allocator: ResourceAllocator, url: Url, metadata: UrlMetadata): Media {
        metadata as VideoIdUrlMetadata

        val requestVariables = buildJsonObject {
            put("shortcode", metadata.videoId)
            put("child_comment_count", 0)
            put("fetch_comment_count", 0)
            put("parent_comment_count", 0)
            put("has_threaded_comments", false)
        }.toString()

        val response = Platform.client.get(Url("https://www.instagram.com/graphql/query/") {
            parameters.append("query_hash", "b3055c01b4b222b8a47dc12b090e4e64")
            parameters.append("variables", requestVariables)
        }).body<JsonElement>()

        val mediaUrl = traversal.traverse(response).first()

        val file = allocator.allocateTempFile(metadata.videoId, MP4)
        Platform.getFile(mediaUrl, file)

        return file.asVideo()
    }

    private fun isInstagramHost(url: Url) = hostPredicate.test(url.host)

    companion object {
        private val hostPredicate = HostPredicate("instagram.com")
        private val traversal = FindUrlWithKeyTraversal("video_url")
    }
}
