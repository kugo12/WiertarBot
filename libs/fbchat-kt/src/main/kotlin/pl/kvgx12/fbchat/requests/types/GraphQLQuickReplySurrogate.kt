package pl.kvgx12.fbchat.requests.types

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import pl.kvgx12.fbchat.data.QuickReplyEmail
import pl.kvgx12.fbchat.data.QuickReplyLocation
import pl.kvgx12.fbchat.data.QuickReplyPhoneNumber
import pl.kvgx12.fbchat.data.QuickReplyText
import pl.kvgx12.fbchat.utils.surrogateDeserializer

@Serializable
private data class GraphQLQuickReplySurrogate(
    @SerialName("content_type")
    val type: String,
    val payload: JsonElement? = null,
    val data: JsonElement? = null,
    @SerialName("image_url")
    val imageUrl: String? = null,
    val title: String? = null,
)

internal val graphQLQuickReplyDeserializer = surrogateDeserializer<GraphQLQuickReplySurrogate, _> {
    when (it.type) {
        "text" -> QuickReplyText(
            payload = it.payload,
            title = it.title,
            externalPayload = null,
            imageUrl = it.imageUrl,
            data = it.data,
            isResponse = false, // TODO: ??
        )

        "location" -> QuickReplyLocation(
            payload = it.payload,
            externalPayload = null,
            data = it.data,
            isResponse = false,
        )

        "user_phone_number" -> QuickReplyPhoneNumber(
            payload = it.payload,
            externalPayload = null,
            data = it.data,
            isResponse = false,
            imageUrl = it.imageUrl,
        )

        "user_email" -> QuickReplyEmail(
            payload = it.payload,
            externalPayload = null,
            data = it.data,
            isResponse = false,
            imageUrl = it.imageUrl,
        )

        else -> throw SerializationException("Unknown type ${it.type} $it")
    }
}
