package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.data.TBotCommandScope


/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#getmycommands)
 */
@Serializable
data class TGetMyCommandsRequest(
    val scope: @Serializable(TBotCommandScope.Companion.Serializer::class) TBotCommandScope? = null,
    @SerialName("language_code")
    val languageCode: String? = null,
)
