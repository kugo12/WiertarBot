package pl.kvgx12.wiertarbot.config

import kotlinx.serialization.Serializable

@ConfigProperties("wiertarbot.sentry.python")
@Serializable
data class PythonSentryProperties(
    val dsn: String,
    val environment: String,
    val sampleRate: Float
)
