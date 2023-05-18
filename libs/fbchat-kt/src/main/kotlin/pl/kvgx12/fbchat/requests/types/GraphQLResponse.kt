package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.Serializable

@Serializable
internal data class GraphQLResponse<T : Any>(
    val data: T? = null,
    val errors: List<GraphQLError> = emptyList(),
)
