package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.NestedJsonListSerializer

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#setwebhook)
 */
@Serializable
data class TSetWebhookRequest(
    val url: String,
    val certificate: String? = null,
    @SerialName("ip_address")
    val ipAddress: String? = null,
    @SerialName("max_connections")
    val maxConnections: Int? = null,
    @SerialName("allowed_updates")
    val allowedUpdates: @Serializable(NestedJsonListSerializer::class) List<String>? = null,
    @SerialName("drop_pending_updates")
    val dropPendingUpdates: Boolean? = null,
    @SerialName("secret_token")
    val secretToken: String? = null,
)
