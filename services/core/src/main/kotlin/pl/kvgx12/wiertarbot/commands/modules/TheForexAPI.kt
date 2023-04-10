package pl.kvgx12.wiertarbot.commands.modules

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object TheForexAPI {
    private const val latestUrl = "https://theforexapi.com/api/latest"

    private val client = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun convert(fromCurrency: String, toCurrency: String, value: Double): Double {
        return value * getRate(fromCurrency, toCurrency)
    }

    suspend fun getRate(fromCurrency: String, toCurrency: String): Double = runCatching {
        client.get(latestUrl) {
            parameter("base", fromCurrency)
            parameter("symbols", toCurrency)
        }.bodyAsText()
            .let { Json.decodeFromString<Response>(it) }
            .rates
            .values
            .first()
    }.fold(
        onSuccess = { it },
        onFailure = { throw InvalidCurrencyException(it) }
    )

    class InvalidCurrencyException(throwable: Throwable) : Exception("Invalid currency", throwable)

    @Serializable
    data class Response(
        val date: String,
        val base: String,
        val rates: Map<String, Double>,
    )
}
