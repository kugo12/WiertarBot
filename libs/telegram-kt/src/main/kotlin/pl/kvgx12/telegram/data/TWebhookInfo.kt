package pl.kvgx12.telegram.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#webhookinfo)
 */
@Serializable
data class TWebhookInfo(
    val url: String,
    @SerialName("has_custom_certificate")
    val hasCustomCertificate: Boolean,
    @SerialName("pending_update_count")
    val pendingUpdateCount: Int,
    @SerialName("ip_address")
    val ipAddress: String? = null,
    @SerialName("last_error_date")
    val lastErrorDate: Int? = null,
    @SerialName("last_error_message")
    val lastErrorMessage: String? = null,
    @SerialName("max_connections")
    val maxConnections: Int? = null,
    @SerialName("allowed_updates")
    val allowedUpdates: List<String>? = null
)
