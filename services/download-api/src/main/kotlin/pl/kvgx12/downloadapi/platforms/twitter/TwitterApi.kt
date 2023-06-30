package pl.kvgx12.downloadapi.platforms.twitter

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.resources.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.downloadapi.utils.Url

@Resource("/")
class TwitterApi {
    @Resource("tweet-result")
    data class TweetResult(
        val parent: TwitterApi = TwitterApi(),
        val id: String,
        val lang: String = "en",
    ) {
        @Serializable
        data class Response(
            val video: Video? = null,
            @SerialName("media_details")
            val mediaDetails: List<MediaDetail> = emptyList(),
        )

        @Serializable
        data class Video(
            val variants: List<VideoVariant>,
        )

        @Serializable
        data class VideoVariant(
            val type: String,
            val src: String,
        )

        @Serializable
        data class MediaDetail(
            val type: String,
            @SerialName("video_info")
            val videoInfo: VideoInfo? = null,
        )

        @Serializable
        data class VideoInfo(
            val variants: List<VideoInfoVariant> = emptyList(),
        )

        @Serializable
        data class VideoInfoVariant(
            val bitrate: Long,
            @SerialName("content_type")
            val contentType: String,
            val url: String,
        )

        companion object {
            suspend fun get(client: HttpClient, id: String) = client.get(
                Url(baseUrl) {
                    client.href(TweetResult(id = id), this)
                },
            ).body<Response>()
        }
    }

    companion object {
        val baseUrl = Url("https://cdn.syndication.twimg.com")
    }
}
