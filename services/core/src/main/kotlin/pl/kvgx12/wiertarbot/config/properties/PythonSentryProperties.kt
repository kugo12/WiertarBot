package pl.kvgx12.wiertarbot.config.properties

import kotlinx.serialization.Serializable
import pl.kvgx12.wiertarbot.config.ConfigProperties

@ConfigProperties("wiertarbot.sentry.python")
@Serializable
data class PythonSentryProperties(
    val dsn: String,
    val environment: String,
    val sampleRate: Float,
)
