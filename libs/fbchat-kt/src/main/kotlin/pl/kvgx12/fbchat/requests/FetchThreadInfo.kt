@file:Suppress("OPT_IN_USAGE")

package pl.kvgx12.fbchat.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import pl.kvgx12.fbchat.data.Thread
import pl.kvgx12.fbchat.data.ThreadData
import pl.kvgx12.fbchat.requests.types.GraphQLResponse
import pl.kvgx12.fbchat.requests.types.GraphQLThread
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.fbchat.session.graphqlRequest

@Serializable
private data class ThreadResponse(
    @SerialName("message_thread")
    val thread: GraphQLThread? = null,
)

private suspend fun fetchThreadInfo(session: Session, thread: Thread): ThreadData? {
    val responseJson = session.graphqlRequest(
        "3449967031715030",
        buildJsonObject {
            put("id", thread.id)
            put("message_limit", "0")
            put("load_messages", false)
            put("load_reac_receipts", false)
            put("before", null)
        },
    )
    Session.log.debug("Fetch thread info: {}", responseJson)

    val response: GraphQLResponse<ThreadResponse> = Session.json.decodeFromString(responseJson)

    val data = response.data?.thread ?: run {
        // FIXME: error checking
        Session.log.error("Failed to fetch thread info: json=$responseJson")
        return null
    }

    val additionalInfo = if (data.threadType == GraphQLThread.SINGLE) {
        fetchAdditionalInfo(session, listOf(thread.id))[thread.id]
    } else {
        null
    }

    return data.toThread(session.userId, additionalInfo)
}

suspend fun Session.fetch(thread: Thread) = fetchThreadInfo(this, thread)
