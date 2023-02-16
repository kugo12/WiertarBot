package pl.kvgx12.wiertarbot.events

data class Response(
    val event: MessageEvent,
    val text: String? = null,
    val files: List<String>? = null,
    val voiceClip: Boolean = false,
    val mentions: List<Mention>? = null,
    val replyToId: String? = null
) {
    suspend fun send() = event.context.sendResponse(this)
    fun pySend() = event.context.pySendResponse(this)
}