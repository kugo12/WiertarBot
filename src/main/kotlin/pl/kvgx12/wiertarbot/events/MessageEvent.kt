package pl.kvgx12.wiertarbot.events

import pl.kvgx12.wiertarbot.connector.ConnectorContext

data class MessageEvent(
    override val context: ConnectorContext,
    val text: String,
    val authorId: String,
    val threadId: String,
    val at: Long,
    val mentions: List<Mention>,
    val externalId: String,
    val replyToId: String?,
    val attachments: List<Attachment>
): Event {
    val isGroup get() = threadId != authorId

    suspend fun react(reaction: String) = context.reactToMessage(this, reaction)
    fun pyReact(reaction: String) = context.pyReactToMessage(this, reaction)

    fun copyWithDifferentText(text: String) = copy(text = text)
}