package pl.kvgx12.wiertarbot.commands.clients.internal

import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange

interface WeatherClient {
    @GetExchange("/weather")
    suspend fun weather(@RequestParam city: String): String
}
