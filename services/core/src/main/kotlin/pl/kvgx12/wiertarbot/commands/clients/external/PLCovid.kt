package pl.kvgx12.wiertarbot.commands.clients.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange("https://services-eu1.arcgis.com/zk7YlClTgerl62BY/arcgis/rest/services")
interface PLCovidStatsClient {
    @GetExchange("/global_corona_actual_widok3/FeatureServer/0/query")
    suspend fun query(
        @RequestParam("f") format: String = "json",
        @RequestParam cacheHint: Boolean = true,
        @RequestParam resultOffset: Int = 0,
        @RequestParam resultRecordCount: Int = 1,
        @RequestParam where: String = "1=1",
        @RequestParam outFields: String = "*",
    ): QueryResponse

    @Serializable
    data class QueryResponse(
        val features: List<Feature>,
    ) {
        @Serializable
        data class Feature(val attributes: Attributes)

        @Serializable
        data class Attributes(
            @SerialName("DATA_SHOW") val date: String,
            @SerialName("ZAKAZENIA_DZIENNE") val dailyInfections: Int,
            @SerialName("ZGONY_DZIENNE") val dailyDeaths: Int,
            @SerialName("LICZBA_OZDROWIENCOW") val dailyRecovered: Int,
            @SerialName("TESTY") val dailyTests: Int,
            @SerialName("TESTY_POZYTYWNE") val dailyPositive: Int,
            @SerialName("KWARANTANNA") val quarantine: Int,
            @SerialName("LICZBA_ZAKAZEN") val totalInfections: Int,
            @SerialName("WSZYSCY_OZDROWIENCY") val totalRecovered: Int,
            @SerialName("LICZBA_ZGONOW") val totalDeaths: Int,
        )
    }
}

class PLCovidStats(private val client: PLCovidStatsClient) {
    suspend fun get(): String {
        val data = client.query()
            .features.first().attributes

        return """
            Statystyki COVID19 w Polsce na ${data.date}:
            Dziennie:
            ${data.dailyInfections} zakażonych
            ${data.dailyDeaths} zgonów
            ${data.dailyRecovered} ozdrowieńców
            ${data.dailyTests} testów
            ${data.dailyPositive} testów pozytywnych
            ${data.quarantine} osób na kwarantannie aktualnie

            Ogółem:
            ${data.totalInfections} zakażonych
            ${data.totalRecovered} ozdrowieńców
            ${data.totalDeaths} zgonów
        """.trimIndent()
    }
}
