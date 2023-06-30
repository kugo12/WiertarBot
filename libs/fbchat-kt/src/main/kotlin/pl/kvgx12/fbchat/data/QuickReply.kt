package pl.kvgx12.fbchat.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
sealed interface QuickReply {
    val payload: JsonElement?
    val externalPayload: JsonElement?
    val data: JsonElement?
    val isResponse: Boolean
}

@Serializable
data class QuickReplyText(
    override val payload: JsonElement?,
    override val externalPayload: JsonElement?,
    override val data: JsonElement?,
    override val isResponse: Boolean,
    val title: String?,
    val imageUrl: String?,
) : QuickReply

@Serializable
data class QuickReplyLocation(
    override val payload: JsonElement?,
    override val externalPayload: JsonElement?,
    override val data: JsonElement?,
    override val isResponse: Boolean,
) : QuickReply

@Serializable
data class QuickReplyPhoneNumber(
    override val payload: JsonElement?,
    override val externalPayload: JsonElement?,
    override val data: JsonElement?,
    override val isResponse: Boolean,
    val imageUrl: String?,
) : QuickReply

@Serializable
data class QuickReplyEmail(
    override val payload: JsonElement?,
    override val externalPayload: JsonElement?,
    override val data: JsonElement?,
    override val isResponse: Boolean,
    val imageUrl: String?,
) : QuickReply
