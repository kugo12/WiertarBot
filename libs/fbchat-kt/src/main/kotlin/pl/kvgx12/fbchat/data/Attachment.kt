package pl.kvgx12.fbchat.data

import kotlinx.serialization.Serializable

@Serializable
sealed interface Attachment {
    val id: String?
}

@Serializable
data class UnknownAttachment(override val id: String?) : Attachment

@Serializable
data class UnsentMessage(override val id: String?) : Attachment

@Serializable
data class ShareAttachment(override val id: String?) : Attachment

@Serializable
data class FileAttachment(
    override val id: String?,
    val url: String?,
    val size: Int?,
    val name: String?,
    val isMalicious: Boolean?,
) : Attachment

@Serializable
data class AudioAttachment(
    override val id: String?,
    val name: String?,
    val url: String?,
    val duration: Long?,
    val audioType: String?,
) : Attachment

@Serializable
data class ImageAttachment(
    override val id: String?,
    val originalExtension: String?,
    val width: Int?,
    val height: Int?,
    val isAnimated: Boolean?,
    val previews: List<Image>,
    val name: String?,
) : Attachment

@Serializable
data class VideoAttachment(
    override val id: String?,
    val size: Int?,
    val width: Int?,
    val height: Int?,
    val duration: Long?,
    val previewUrl: String?,
    val previews: List<Image>,
) : Attachment

@Serializable
data class LocationAttachment(override val id: String?) : Attachment

@Serializable
data class LiveLocationAttachment(override val id: String?) : Attachment

@Serializable
data class Sticker(
    override val id: String?,
    val pack: String?,
    val isAnimated: Boolean?,
    val mediumSpriteImage: String?,
    val largeSpriteImage: String?,
    val framesPerRow: Int?,
    val framesPerCol: Int?,
    val frameCount: Int?,
    val frameRate: Int?,
    val image: Image?,
    val label: String?,
) : Attachment
