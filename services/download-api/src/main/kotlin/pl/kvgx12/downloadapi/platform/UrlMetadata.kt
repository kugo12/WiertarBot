package pl.kvgx12.downloadapi.platform

import io.ktor.http.*
import pl.kvgx12.downloadapi.utils.base64

sealed interface UrlMetadata {
    val url: Url
    val extension: String

    val filename get() = "${url.base64()}$extension"
}

data class EmptyMetadata(
    override val url: Url,
    override val extension: String = FileExtensions.MP4,
) : UrlMetadata

data class VideoIdUrlMetadata(
    override val url: Url,
    val videoId: String,
) : UrlMetadata {
    override val extension: String get() = FileExtensions.MP4
    override val filename: String get() = "$videoId$extension"
}

object FileExtensions {
    const val MP4 = ".mp4"
}
