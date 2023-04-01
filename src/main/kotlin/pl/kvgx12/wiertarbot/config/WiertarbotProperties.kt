package pl.kvgx12.wiertarbot.config

@ConfigProperties("wiertarbot")
data class WiertarbotProperties(
    val prefix: String = "!",
    val timezone: String = "Europe/Warsaw",
    val catApi: CatApi = CatApi(),
    val rabbitMQExchange: String = "bot.default"
) {
    data class CatApi(
        val key: String? = null
    )
}
