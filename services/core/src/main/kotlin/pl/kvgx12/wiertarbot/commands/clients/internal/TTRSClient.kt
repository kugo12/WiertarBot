package pl.kvgx12.wiertarbot.commands.clients.internal

import kotlinx.serialization.Serializable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.PostExchange

interface TTRSClient {
    @PostExchange("/api/tts")
    suspend fun textToSpeech(request: TTSRequest): ByteArray

    @GetExchange("/api/tts/langs")
    suspend fun textToSpeechLanguages(): Map<String, String>

    @PostExchange("/api/translate")
    suspend fun translate(request: TranslateRequest): String

    @Serializable
    data class TranslateRequest(
        val text: String,
        val source: String?,
        val destination: String,
    )

    @Serializable
    data class TTSRequest(
        val text: String,
        val lang: String?,
    )
}
