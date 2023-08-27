package pl.kvgx12.wiertarbot.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("wiertarbot.weather", ignoreInvalidFields = true)
data class WeatherProperties(val url: String)
