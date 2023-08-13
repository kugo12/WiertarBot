package pl.kvgx12.wiertarbot.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("wiertarbot.ttrs", ignoreInvalidFields = true)
data class TTRSProperties(
    val url: String,
) {
    val ttsUrl = "$url/api/tts"
    val ttsLangUrl = "$url/api/tts/lang"
    val translateUrl = "$url/api/translate"
}
