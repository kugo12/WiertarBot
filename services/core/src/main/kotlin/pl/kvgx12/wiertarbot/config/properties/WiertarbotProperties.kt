package pl.kvgx12.wiertarbot.config.properties

import pl.kvgx12.wiertarbot.config.ConfigProperties

@ConfigProperties("wiertarbot")
data class WiertarbotProperties(
    val prefix: String = "!",
    val timezone: String = "Europe/Warsaw",
    val rabbitMQExchange: String = "bot.default",
)
