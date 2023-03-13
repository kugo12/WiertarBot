package pl.kvgx12.wiertarbot.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("wiertarbot.telegram")
@ConditionalOnProperty("wiertarbot.telegram.enabled", havingValue = "true")
data class TelegramProperties(
    val token: String
)