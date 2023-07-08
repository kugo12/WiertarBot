package pl.kvgx12.fbchat.session

import io.ktor.client.plugins.resources.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import pl.kvgx12.fbchat.utils.tryGet

internal suspend fun Session.plainGraphQLRequest(
    docId: String,
    variables: String,
) = post(
    Messenger.Api.GraphQL(),
    mapOf(
        "doc_id" to docId,
        "variables" to variables,
    ),
)

internal suspend fun Session.plainGraphQLBatchRequest(
    docId: String,
    variables: JsonElement,
) = post(
    Messenger.Api.GraphQLBatch(),
    mapOf(
        "method" to "GET",
        "response_format" to "json",
        "queries" to buildJsonObject {
            putJsonObject("q0") {
                put("doc_id", docId)
                put("query_params", variables)
            }
        }.toString(),
    ),
).splitToSequence("\r\n")
    .map { Session.json.parseToJsonElement(it.trimStart()) }
    .first()
    .tryGet("q0")!!

internal suspend fun Session.plainGraphQLMutation(
    docId: String,
    variables: String,
) = payloadPost(
    client.href(Messenger.WebGraphQL.Mutation()),
    mapOf(
        "doc_id" to docId,
        "variables" to variables,
    ),
)

internal suspend inline fun <reified T> Session.graphqlRequest(
    docId: String,
    variables: T,
) = plainGraphQLRequest(docId, Session.json.encodeToString(variables))

internal suspend inline fun <reified T> Session.graphqlBatchRequest(
    docID: String,
    data: T,
) = plainGraphQLBatchRequest(docID, Session.json.encodeToJsonElement(data))

internal suspend inline fun <reified T> Session.graphqlMutation(
    docId: String,
    variables: T,
) = plainGraphQLMutation(docId, Session.json.encodeToString(variables))
