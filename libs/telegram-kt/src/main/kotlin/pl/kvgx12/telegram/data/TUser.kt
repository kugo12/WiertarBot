package pl.kvgx12.telegram.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#user)
 */
@Serializable
data class TUser(
    val id: Long,
    @SerialName("is_bot")
    val isBot: Boolean,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String? = null,
    val username: String? = null,
)

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#userprofilephotos)
 */
@Serializable
data class TUserProfilePhotos(
    @SerialName("total_count")
    val totalCount: Long,
    val photos: List<List<TPhotoSize>>,
)
