package pl.kvgx12.telegram.data.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import pl.kvgx12.telegram.data.TBotCommand
import pl.kvgx12.telegram.data.TBotCommandScope

/**
 * [Telegram API Docs](https://core.telegram.org/bots/api#setmycommands)
 */
@Serializable
data class TSetMyCommandsRequest(
    val commands: List<TBotCommand>,
    val scope: TBotCommandScope? = null,
    @SerialName("language_code")
    val languageCode: String? = null,
) {
    init {
        require(commands.size <= 100) { "The commands list must not contain more than 100 commands." }
    }
}
