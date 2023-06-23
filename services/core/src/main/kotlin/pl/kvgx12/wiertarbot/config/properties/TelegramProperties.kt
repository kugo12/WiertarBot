package pl.kvgx12.wiertarbot.config.properties

import pl.kvgx12.wiertarbot.config.ConfigProperties


@ConfigProperties("wiertarbot.telegram")
data class TelegramProperties(
    val token: String
)
