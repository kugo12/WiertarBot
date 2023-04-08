package pl.kvgx12.wiertarbot.config


@ConfigProperties("wiertarbot.telegram")
data class TelegramProperties(
    val token: String
)