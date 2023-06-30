package pl.kvgx12.fbchat.requests

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import pl.kvgx12.fbchat.data.Message
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.fbchat.session.graphqlMutation

private suspend fun reactToMessage(session: Session, message: Message, reaction: String?) {
    session.graphqlMutation(
        "1491398900900362",
        buildJsonObject {
            putJsonObject("data") {
                put("action", if (reaction != null) "ADD_REACTION" else "REMOVE_REACTION")
                put("client_mutation_id", "1")
                put("actor_id", session.userId)
                put("message_id", message.id)
                put("reaction", reaction)
            }
        },
    )
    // TODO handle failures
}

suspend fun Message.react(session: Session, reaction: String?) = reactToMessage(session, this, reaction)
