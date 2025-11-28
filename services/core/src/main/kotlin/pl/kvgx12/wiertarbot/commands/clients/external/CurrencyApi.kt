package pl.kvgx12.wiertarbot.commands.clients.external

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1")
interface CurrencyApiClient {
    @GetExchange("/currencies/{base}.json")
    suspend fun latest(@PathVariable base: String): JsonObject  // date, {base}
}

class CurrencyApi(private val client: CurrencyApiClient) {
    suspend fun convert(fromCurrency: String, toCurrency: String, value: Double): Double {
        return value * getRate(fromCurrency, toCurrency)
    }

    private suspend fun getRate(fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency) {
            return 1.0
        }

        val response = try {
            client.latest(fromCurrency)
        } catch (e: WebClientResponseException.NotFound) {
            throw InvalidCurrencyException(e)
        }

        return response[fromCurrency]?.jsonObject[toCurrency]?.jsonPrimitive?.double
            ?: throw InvalidCurrencyException()
    }

    class InvalidCurrencyException(throwable: Throwable? = null) : Exception("Invalid currency", throwable)
}
