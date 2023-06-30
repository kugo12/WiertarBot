package pl.kvgx12.downloadapi.platforms.youtube

import io.ktor.http.*
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import pl.kvgx12.downloadapi.platform.*
import pl.kvgx12.downloadapi.platform.FileExtensions.MP4
import pl.kvgx12.downloadapi.platforms.*
import pl.kvgx12.downloadapi.utils.HostPredicate
import pl.kvgx12.downloadapi.utils.lastPathSegment

@ConfigurationProperties("platform.youtube")
data class YoutubeProperties(
    val clientVersion: String,
    val clientName: String,
)

@EnableConfigurationProperties(YoutubeProperties::class)
class YoutubePlatform(private val properties: YoutubeProperties) : Platform {
    override val name: String get() = "youtube"

    override suspend fun tryParsing(url: Url): UrlMetadata? {
        if (!isHostValid(url)) return null

        return extractId(url)?.let {
            VideoIdUrlMetadata(url, it)
        }
    }

    override suspend fun download(
        allocator: ResourceAllocator,
        url: Url,
        metadata: UrlMetadata,
    ): Media {
        metadata as VideoIdUrlMetadata

        val videoUrl = YoutubeApi.Player.request(
            metadata.videoId,
            properties.clientName,
            properties.clientVersion,
        )
            .getOrThrow()
            .streamingData.formats
            .asSequence()
            .filter { it.audioSampleRate != null && it.width != null }
            .maxBy { it.width!! }
            .url

        val file = allocator.allocateTempFile(metadata.videoId, MP4)
        Platform.getFileParallel(
            Url("$videoUrl&range=0-100000000"),
            file,
        )

        return Media.Video(file)
    }

    private fun isHostValid(url: Url) = hostPredicate.test(url.host)

    private fun extractId(url: Url): String? =
        (url.parameters["v"] ?: url.lastPathSegment).ifEmpty { null }

    companion object {
        private val hostPredicate = HostPredicate("youtube.com", "youtu.be")
    }
}
