package pl.kvgx12.wiertarbot.commands.clients.external

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange("https://shibe.online/api")
interface ShibeOnlineApiClient {
    @GetExchange("/shibes")
    suspend fun getShibes(
        @RequestParam count: Int,
        @RequestParam urls: Boolean,
        @RequestParam httpsUrls: Boolean,
    ): List<String>
}
