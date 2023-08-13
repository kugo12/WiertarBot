package pl.kvgx12.wiertarbot.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("wiertarbot.download-api", ignoreInvalidFields = true)
data class DownloadApiProperties(val url: String)
