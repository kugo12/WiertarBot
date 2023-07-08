package pl.kvgx12.fbchat.requests

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import pl.kvgx12.fbchat.data.Message
import pl.kvgx12.fbchat.data.MessageData
import pl.kvgx12.fbchat.requests.types.GraphQLMessage
import pl.kvgx12.fbchat.requests.types.GraphQLResponse
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.fbchat.session.graphqlBatchRequest

@Serializable
private data class MessageResponse(val message: GraphQLMessage?)

private suspend fun fetchMessage(message: Message, session: Session): MessageData {
    val response: GraphQLResponse<MessageResponse> = session.graphqlBatchRequest(
        "1768656253222505",
        buildJsonObject {
            putJsonObject("thread_and_message_id") {
                put("thread_id", message.thread.id)
                put("message_id", message.id)
            }
        },
    ).let(Session.json::decodeFromJsonElement)

    return response.data!!.message!!.toMessageData(message.thread)
}

suspend fun Session.fetch(message: Message) = fetchMessage(message, this)
