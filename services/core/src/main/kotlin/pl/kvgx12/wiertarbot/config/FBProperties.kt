package pl.kvgx12.wiertarbot.config

import kotlinx.serialization.Serializable

@ConfigProperties("wiertarbot.fb")
@Serializable
data class FBProperties(
    val password: String,
    val email: String
)
