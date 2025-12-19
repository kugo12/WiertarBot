package pl.kvgx12.wiertarbot.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import pl.kvgx12.wiertarbot.config.PostgresCacheConfig

@ConfigurationProperties("wiertarbot")
data class WiertarbotProperties(
    val prefix: String = "!",
    val timezone: String = "Europe/Warsaw",
    val caches: List<PostgresCacheConfig> = emptyList(),
)
