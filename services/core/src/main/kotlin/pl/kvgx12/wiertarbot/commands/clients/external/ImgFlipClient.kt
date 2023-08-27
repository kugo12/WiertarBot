package pl.kvgx12.wiertarbot.commands.clients.external

import kotlinx.serialization.Serializable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange("https://api.imgflip.com")
interface ImgFlipClient {
    @GetExchange("/get_memes")
    suspend fun getMemes(): Memes

    @Serializable
    data class Memes(
        val success: Boolean,
        val data: Data,
    ) {
        @Serializable
        data class Data(val memes: List<Meme>)

        @Serializable
        data class Meme(val name: String, val url: String)
    }
}
