package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.data.TBotCommandScope

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#deletemycommands)
 */
@Serializable
data class TDeleteMyCommandsRequest(
    val scope: TBotCommandScope? = null,
    val languageCode: String? = null,
)
