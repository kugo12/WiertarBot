package pl.kvgx12.wiertarbot.config.properties

import pl.kvgx12.wiertarbot.config.ConfigProperties

@ConfigProperties("wiertarbot.ttrs")
data class TTRSProperties(
    val url: String
) {
    val ttsUrl = "$url/api/tts"
    val ttsLangUrl = "$url/api/tts/lang"
    val translateUrl = "$url/api/translate"
}
