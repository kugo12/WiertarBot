package pl.kvgx12.wiertarbot.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("wiertarbot")
data class WiertarbotProperties(
    val prefix: String = "!",
    val timezone: String = "Europe/Warsaw",
)
