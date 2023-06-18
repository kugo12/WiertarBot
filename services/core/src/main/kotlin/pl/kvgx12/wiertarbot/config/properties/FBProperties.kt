package pl.kvgx12.wiertarbot.config.properties

import kotlinx.serialization.Serializable
import pl.kvgx12.wiertarbot.config.ConfigProperties

@ConfigProperties("wiertarbot.fb")
@Serializable
data class FBProperties(
    val password: String,
    val email: String
)
