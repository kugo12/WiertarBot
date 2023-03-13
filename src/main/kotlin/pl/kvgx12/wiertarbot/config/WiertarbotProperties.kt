package pl.kvgx12.wiertarbot.config

import kotlinx.serialization.Serializable

@ConfigProperties("wiertarbot")
@Serializable
data class WiertarbotProperties(
    val sentry: Sentry = Sentry(),
    val email: String,
    val password: String,
    val prefix: String = "!",
    val timezone: String = "Europe/Warsaw",
    val catApi: CatApi = CatApi(),
    val rabbitMQExchange: String = "bot.default"
) {
    @Serializable
    data class CatApi(
        val key: String? = null
    )

    @Serializable
    data class Sentry(
        val jvm: Properties? = null,
        val python: Properties? = null,
    ) {
        @Serializable
        data class Properties(
            val dsn: String,
            val environment: String,
            val sampleRate: Float
        )
    }
}