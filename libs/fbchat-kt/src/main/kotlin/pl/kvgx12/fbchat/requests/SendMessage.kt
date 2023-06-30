package pl.kvgx12.fbchat.requests

import pl.kvgx12.fbchat.data.*
import pl.kvgx12.fbchat.session.Session

private val Thread.keyId: String
    get() = when (this) {
        is Group -> "thread_fbid"
        is User, is Page -> "other_user_fbid"
        is UnknownThread -> throw UnsupportedOperationException()
    }

private fun MutableMap<String, String>.putMention(index: Int, mention: Mention) {
    put("profile_xmd[$index][id]", mention.user.id)
    put("profile_xmd[$index][offset]", mention.offset.toString())
    put("profile_xmd[$index][length]", mention.length.toString())
    put("profile_xmd[$index][type]", "p")
}

suspend fun Session.sendMessage(
    thread: Thread,
    text: String? = null,
    mentions: List<Mention> = emptyList(),
    files: List<Pair<String, String>> = emptyList(),
    replyTo: Message? = null,
): MessageId {
    require(replyTo?.let { it.thread.id == thread.id } ?: true)
    require(thread !is UnknownThread)

    val data = buildMap {
        put(thread.keyId, thread.id)
        put("action_type", "ma-type:user-generated-message")

        if (text != null) put("body", text)
        if (files.isNotEmpty()) put("has_attachment", "true")
        if (replyTo != null) put("replied_to_message_id", replyTo.id)

        mentions.forEachIndexed { index, mention ->
            putMention(index, mention)
        }
        files.forEachIndexed { index, (id, type) ->
            put("${contentTypeToIdKey(type)}s[$index]", id)
        }
    }

    val response = doSendRequest(data)

    return MessageId(thread, response.first)
}
