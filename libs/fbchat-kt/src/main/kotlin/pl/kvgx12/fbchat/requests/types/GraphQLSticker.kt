package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.fbchat.data.Image
import pl.kvgx12.fbchat.data.Sticker

@Serializable
internal data class GraphQLSticker(
    val id: String,
    val pack: Pack? = null,
    @SerialName("sprite_image")
    val spriteImage: SpriteImage? = null,
    @SerialName("sprite_image_2x")
    val spriteImage2x: SpriteImage? = null,
    @SerialName("frames_per_row")
    val framesPerRow: Int? = null,
    @SerialName("frames_per_column")
    val framesPerColumn: Int? = null,
    @SerialName("frame_count")
    val frameCount: Int? = null,
    @SerialName("frame_rate")
    val frameRate: Int? = null,
    val label: String? = null,
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
) {
    @Serializable
    data class SpriteImage(
        val uri: String,
    )

    @Serializable
    data class Pack(val id: String)

    fun toSticker() = Sticker(
        id = id,
        pack = pack?.id,
        isAnimated = spriteImage != null,
        mediumSpriteImage = spriteImage?.uri,
        largeSpriteImage = spriteImage2x?.uri,
        framesPerRow = framesPerRow,
        framesPerCol = framesPerColumn,
        frameCount = frameCount,
        frameRate = frameRate,
        image = url?.let { Image(it, width, height) },
        label = label,
    )
}
