package pl.kvgx12.fbchat.requests

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.fbchat.session.graphqlRequest
import pl.kvgx12.fbchat.utils.tryAsString
import pl.kvgx12.fbchat.utils.tryGet

internal suspend fun Session.fetchSequenceId(): String {
    val response = graphqlRequest("1349387578499440", buildJsonObject {
        put("limit", 1)
        putJsonArray("tags") {
            add("INBOX")
        }
        put("before", null)
        put("includeDeliveryReceipts", true)
        put("includeSeqID", true)
    })

    val sequenceId = Session.json.parseToJsonElement(response)
        .tryGet("data")
        .tryGet("viewer")
        .tryGet("message_threads")
        .tryGet("sync_sequence_id")
        .tryAsString()
        ?: error("Did not find sequence id in response: $response")

    this.sequenceId = sequenceId

    return sequenceId
}
