package pl.kvgx12.wiertarbot.commands.clients.internal

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

interface DownloadClient {
    @GetExchange
    suspend fun download(@RequestParam url: String): String

    @GetExchange("/platforms")
    suspend fun supportedPlatforms(): List<String>
}
