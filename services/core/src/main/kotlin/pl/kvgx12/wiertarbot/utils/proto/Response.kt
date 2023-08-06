package pl.kvgx12.wiertarbot.utils.proto

import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.proto.Response

suspend fun Response.send() = event.context.sendResponse(this)

fun Response(
    event: MessageEvent,
    text: String? = null,
    files: List<UploadedFile>? = null,
    voiceClip: Boolean = false,
    mentions: List<Mention>? = null,
    replyToId: String? = null,
) = response {
    this.event = event
    if (text != null) this.text = text
    if (files != null) this.files += files
    this.voiceClip = voiceClip
    if (mentions != null) this.mentions += mentions
    if (replyToId != null) this.replyToId = replyToId
}
