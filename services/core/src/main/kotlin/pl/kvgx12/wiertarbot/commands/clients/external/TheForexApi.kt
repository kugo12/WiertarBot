package pl.kvgx12.wiertarbot.commands.clients.external

import kotlinx.serialization.Serializable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange("https://theforexapi.com")
interface TheForexApiClient {
    @GetExchange("/api/latest")
    suspend fun latest(
        @RequestParam base: String,
        @RequestParam symbols: String,
    ): Response

    @Serializable
    data class Response(
        val date: String,
        val base: String,
        val rates: Map<String, Double>,
    )
}

class TheForexApi(private val client: TheForexApiClient) {
    suspend fun convert(fromCurrency: String, toCurrency: String, value: Double): Double {
        return value * getRate(fromCurrency, toCurrency)
    }

    private suspend fun getRate(fromCurrency: String, toCurrency: String): Double = runCatching {
        val rates = client.latest(fromCurrency, toCurrency)
            .rates

        if (fromCurrency == toCurrency) 1.0 else rates.getValue(toCurrency)
    }.fold(
        onSuccess = { it },
        onFailure = { throw InvalidCurrencyException(it) },
    )

    class InvalidCurrencyException(throwable: Throwable? = null) : Exception("Invalid currency", throwable)
}
