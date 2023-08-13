package pl.kvgx12.wiertarbot.fb.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("wiertarbot.fb")
data class FBProperties(
    val password: String,
    val email: String,
)
