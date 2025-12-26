package pl.kvgx12.telegram.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#reactiontype]
 */
@Serializable
sealed interface TReactionType {
    @Serializable
    @SerialName("emoji")
    data class Emoji(val emoji: String) : TReactionType

    @Serializable
    @SerialName("custom_emoji")
    data class CustomEmojiId(@SerialName("custom_emoji_id") val customEmojiId: String) : TReactionType

    @Serializable
    @SerialName("paid")
    data object Paid : TReactionType
}
