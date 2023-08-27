package pl.kvgx12.wiertarbot.connector.utils.proto

import pl.kvgx12.wiertarbot.proto.Mention
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.UploadedFile
import pl.kvgx12.wiertarbot.proto.response

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
