package pl.kvgx12.wiertarbot.events

open class Attachment(
    val id: String?,
)

class ImageAttachment(
    id: String?,
    val width: Int?,
    val height: Int?,
    val originalExtension: String?,
    val isAnimated: Boolean?,
): Attachment(id)