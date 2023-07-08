package pl.kvgx12.downloadapi.platforms.twitter

import io.ktor.http.*
import pl.kvgx12.downloadapi.platform.*
import pl.kvgx12.downloadapi.platform.FileExtensions.MP4
import pl.kvgx12.downloadapi.utils.HostPredicate
import pl.kvgx12.downloadapi.utils.getSegmentAfter

class TwitterPlatform : Platform {
    override val name: String get() = "twitter"

    override suspend fun tryParsing(url: Url): UrlMetadata? {
        if (!isTwitterHost(url)) {
            return null
        }

        return getTweetId(url)?.let {
            VideoIdUrlMetadata(url, it)
        }
    }

    override suspend fun download(allocator: ResourceAllocator, url: Url, metadata: UrlMetadata): Media {
        metadata as VideoIdUrlMetadata

        val response = TwitterApi.TweetResult.get(Platform.client, metadata.videoId)

        val video = response.mediaDetails.tryExtractingUrl()
            ?: response.video?.tryExtractingUrl()
            ?: error("Couldn't find video url from $url in $response")

        val videoFile = allocator.allocateTempFile(metadata.videoId, MP4)

        Platform.getFile(Url(video), videoFile)

        return Media.Video(videoFile)
    }

    private fun isTwitterHost(url: Url): Boolean = hostPredicate.test(url.host)

    private suspend fun getTweetId(url: Url): String? =
        if (shortHostPredicate.test(url.host)) {
            Platform.followRedirects(url).idSegment
        } else {
            url.idSegment
        }

    private inline val Url.idSegment: String? get() = getSegmentAfter("status")

    private fun List<TwitterApi.TweetResult.MediaDetail>.tryExtractingUrl(): String? =
        firstOrNull { it.type == "video" }
            ?.videoInfo?.variants
            ?.filter { it.contentType == ContentType.Video.MP4.toString() }
            ?.maxByOrNull { it.bitrate }
            ?.url

    private fun TwitterApi.TweetResult.Video.tryExtractingUrl(): String? =
        variants.firstOrNull { it.type == ContentType.Video.MP4.toString() }?.src

    companion object {
        private val hostPredicate = HostPredicate("twitter.com", "t.co")
        private val shortHostPredicate = HostPredicate("t.co")
    }
}
