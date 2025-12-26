package pl.kvgx12.wiertarbot.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("wiertarbot.cex", ignoreInvalidFields = true)
class CEXProperties(
    val apiKey: String,
    val url: String,
    val timeout: Long,  // seconds
)
