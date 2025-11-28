package pl.kvgx12.wiertarbot.fb.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("wiertarbot.fb")
data class FBProperties(
    val cookiesFile: String = "cookies.json"
)
