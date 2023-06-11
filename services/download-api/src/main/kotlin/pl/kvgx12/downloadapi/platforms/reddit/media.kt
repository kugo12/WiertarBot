package pl.kvgx12.downloadapi.platforms.reddit

import kotlinx.serialization.Serializable

@Serializable
class MediaEmbed

@Serializable
class SecureMediaEmbed

@Serializable
class SecureMedia(
    val isGif: Boolean = false,
    val hlsUrl: String,
    val dashUrl: String,
    val duration: Int,
    val width: Int,
    val height: Int,
    val fallbackUrl: String,
    val bitrateKbps: Int,
    val transcodingStatus: String,
)

@Serializable
class Media
