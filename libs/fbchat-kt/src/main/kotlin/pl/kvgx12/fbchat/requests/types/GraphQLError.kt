package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.Serializable

@Serializable
internal data class GraphQLError(
    val message: String = "",
    val severity: String = ""
)
