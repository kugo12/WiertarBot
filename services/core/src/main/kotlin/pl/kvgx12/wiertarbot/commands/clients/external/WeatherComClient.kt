package pl.kvgx12.wiertarbot.commands.clients.external

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.AirQualityResponse
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.AlertsResponse
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.CurrentObservationsResponse
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.DailyForecastResponse
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.GeoCode
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.GeoCodedParameters
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.ObjectListSerializer
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.PollenResponse
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.QueryParameters
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.Request
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.Response
import pl.kvgx12.wiertarbot.commands.clients.external.WeatherComDto.SearchLocationResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@HttpExchange("https://weather.com/api/v1/p/redux-dal")
interface WeatherComApi {
    @PostExchange
    suspend fun post(@RequestBody requests: List<Request>): Response
}

class WeatherComClient(private val api: WeatherComApi) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }


    suspend fun weather(city: String): String = formatMessage(getDetailedWeather(getCoordinates(city)))

    private suspend fun getCoordinates(city: String): GeoCode {
        val request = Request(
            GET_LOCATION,
            QueryParameters(city, "pl-PL", "locale")
        )

        val response = api.post(listOf(request))
        val location = response.decodeFirst<SearchLocationResponse>(GET_LOCATION)
            ?.location?.firstOrNull()
            ?: throw CityNotFoundException(city)

        return GeoCode(
            name = location.displayName,
            latitude = location.latitude,
            longitude = location.longitude,
            shortName = location.city,
        )
    }

    private suspend fun getDetailedWeather(geo: GeoCode): DetailedWeatherAggregate {
        val params = GeoCodedParameters("${geo.latitude},${geo.longitude}", "m", "pl-PL")

        val response = api.post(
            listOf(
                Request(GET_ALERT_HEADLINES, params),
                Request(GET_DAILY_FORECAST, params.copy(duration = "7day")),
                Request(GET_CURRENT_OBSERVATIONS, params),
                Request(GET_POLLEN, params.copy(duration = "3day")),
                Request(GET_AIR_QUALITY, params.copy(scale = "EPA", units = ""))
            )
        )

        return DetailedWeatherAggregate(
            geoCode = geo,
            alerts = response.decodeFirst(GET_ALERT_HEADLINES),
            dailyForecast = response.decodeFirst(GET_DAILY_FORECAST),
            current = response.decodeFirst(GET_CURRENT_OBSERVATIONS),
            pollen = response.decodeFirst(GET_POLLEN),
            airQuality = response.decodeFirst(GET_AIR_QUALITY)
        )
    }

    private fun formatMessage(a: DetailedWeatherAggregate): String {
        val warsaw = ZoneId.of("Europe/Warsaw")
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        return buildString {
            a.current?.let { current ->
                appendLine("Pogoda w ${a.geoCode.shortName}: ${current.wxPhraseLong}")
                appendLine("Temperatura: ${current.temperature}°C, odczuwalna ${current.temperatureFeelsLike}°C")
                appendLine("Wiatr: ${current.windSpeed} km/h")
                appendLine("Ciśnienie: %.1f hPa".format(current.pressureAltimeter))
                appendLine("Wilgotność: ${current.relativeHumidity}%")
                appendLine("Widoczność: %.1f km".format(current.visibility))
                appendLine("Zachmurzenie: ${current.cloudCoverPhrase}")
                appendLine("Opady 6h: %.1f mm".format(current.precip6Hour))

                val sunrise = current.sunriseTimeUtc?.let { Instant.ofEpochSecond(it).atZone(warsaw).format(formatter) } ?: "?"
                val sunset = current.sunsetTimeUtc?.let { Instant.ofEpochSecond(it).atZone(warsaw).format(formatter) } ?: "?"
                appendLine("Wschód $sunrise, zachód $sunset")
            }

            a.pollen?.pollenForecast12hour?.let {
                val pollen = it.firstOrNull() ?: return@let
                appendLine(
                    "Pylenie: trawy - ${pollen.grassPollenCategory}, " +
                        "drzewa - ${pollen.treePollenCategory}, " +
                        "chwasty - ${pollen.ragweedPollenCategory}"
                )
            }

            a.airQuality?.globalAirQuality?.let { aq ->
                append("Jakość powietrza: ${aq.airQualityCategory} (")
                aq.pollutants?.values?.joinTo(this, ", ") { pollutant ->
                    "${pollutant.name} - %.0f%s".format(pollutant.amount, pollutant.unit)
                }
                appendLine(")")
            }

            a.dailyForecast?.let { forecast ->
                val rain = forecast.firstOrNull()
                    ?.let(::getChances)
                    ?: return@let
                appendLine("Szansa na opady w najbliższym czasie: $rain%")
            }

            a.alerts?.alerts?.takeIf { it.isNotEmpty() }?.let { alerts ->
                appendLine("Ostrzeżenia:")
                alerts.forEach { alert ->
                    appendLine("- ${alert.eventDescription} (${alert.certainty}, ${alert.urgency}, ${alert.severity})")
                }
            }
        }
    }

    private fun getChances(forecast: DailyForecastResponse): Int? {
        forecast.dayPart.forEach { day ->
            if (day.precipChance != null) {
                return day.precipChance
            }
        }
        return null
    }

    private inline fun <reified T> Response.decodeFirst(requestName: String): T? {
        val responses = dal[requestName] ?: return null

        for (response in responses.values) {
            if (response.status == 200 && response.data != null) {
                println(response.data)
                return json.decodeFromJsonElement<T>(response.data)
            }
        }

        return null
    }

    data class DetailedWeatherAggregate(
        val geoCode: GeoCode,
        val alerts: AlertsResponse? = null,
        val dailyForecast: @Serializable(ObjectListSerializer::class) List<DailyForecastResponse>? = null,
        val current: CurrentObservationsResponse? = null,
        val pollen: PollenResponse? = null,
        val airQuality: AirQualityResponse? = null
    )

    class CityNotFoundException(val city: String) : Exception("City not found: $city")

    companion object {
        private const val GET_LOCATION = "getSunV3LocationSearchUrlConfig"
        private const val GET_ALERT_HEADLINES = "getSunWeatherAlertHeadlinesUrlConfig"
        private const val GET_POLLEN = "getSunIndexPollenDaypartUrlConfig"
        private const val GET_CURRENT_OBSERVATIONS = "getSunV3CurrentObservationsUrlConfig"
        private const val GET_DAILY_FORECAST = "getSunV3DailyForecastWithHeadersUrlConfig"
        private const val GET_AIR_QUALITY = "getSunV3GlobalAirQualityUrlConfig"
    }
}

object WeatherComDto {
    class ObjectListSerializer<T>(serializer: KSerializer<T>) : JsonTransformingSerializer<List<T>>(ListSerializer(serializer)) {
        override fun transformDeserialize(element: JsonElement): JsonElement {
            val obj = element.jsonObject
            val size = obj.entries.maxOf { (_, value) ->
                if (value is JsonArray) value.size else 0
            }

            val list = mutableListOf<JsonElement>()
            for (i in 0 until size) {
                buildJsonObject {
                    for ((key, value) in obj.entries) {
                        if (value is JsonArray && i < value.size) put(key, value[i])
                    }
                }.let(list::add)
            }

            return JsonArray(list)
        }
    }

    @Serializable
    data class Request(val name: String, val params: RequestParams)

    @Serializable
    sealed interface RequestParams

    @Serializable
    data class QueryParameters(
        val query: String, val language: String, val locationType: String
    ) : RequestParams

    @Serializable
    data class GeoCodedParameters(
        @SerialName("geocode") val geoCode: String,
        val units: String? = null,
        val duration: String? = null,
        val language: String? = null,
        val scale: String? = null,
        val par: String? = null,
        val insightType: String? = null
    ) : RequestParams

    @Serializable
    data class Response(val dal: Map<String, Map<String, ResponseDalData>> = emptyMap())

    @Serializable
    data class ResponseDalData(
        val data: JsonElement? = null,
        val status: Int,
    )

    @Serializable
    data class SearchLocationResponse(val location: @Serializable(ObjectListSerializer::class) List<LocationData> = emptyList())

    @Serializable
    data class LocationData(
        val address: String,
        val city: String,
        val displayName: String,
        val country: String,
        val latitude: Double,
        val longitude: Double,
    )

    @Serializable
    data class PollenResponse(val pollenForecast12hour: @Serializable(ObjectListSerializer::class) List<PollenForecast12Hour> = emptyList())

    @Serializable
    data class PollenForecast12Hour(
        val dayInd: String?,
        val daypartName: String?,
        val grassPollenCategory: String?,
        val grassPollenIndex: Int?,
        val ragweedPollenCategory: String?,
        val ragweedPollenIndex: Int?,
        val treePollenCategory: String?,
        val treePollenIndex: Int?
    )

    @Serializable
    data class CurrentObservationsResponse(
        val cloudCoverPhrase: String? = null,
        val dayOfWeek: String? = null,
        val dayOrNight: String? = null,
        val expirationTimeUtc: Int? = null,
        val iconCode: Int? = null,
        val iconCodeExtend: Int? = null,
        val obsQualifierCode: String? = null,
        val obsQualifierSeverity: Int? = null,
        val precip1Hour: Double? = null,
        val precip6Hour: Double? = null,
        val precip24Hour: Double? = null,
        val pressureAltimeter: Double? = null,
        val pressureChange: Double? = null,
        val pressureMeanSeaLevel: Double? = null,
        val pressureTendencyCode: Int? = null,
        val pressureTendencyTrend: String? = null,
        val relativeHumidity: Int? = null,
        val snow1Hour: Double? = null,
        val snow6Hour: Double? = null,
        val snow24Hour: Double? = null,
        val sunriseTimeLocal: String? = null,
        val sunriseTimeUtc: Long? = null,
        val sunsetTimeLocal: String? = null,
        val sunsetTimeUtc: Long? = null,
        val temperature: Int? = null,
        val temperatureChange24Hour: Int? = null,
        val temperatureDewPoint: Int? = null,
        val temperatureFeelsLike: Int? = null,
        val temperatureHeatIndex: Int? = null,
        val temperatureMax24Hour: Int? = null,
        val temperatureMaxSince7Am: Int? = null,
        val temperatureMin24Hour: Int? = null,
        val temperatureWindChill: Int? = null,
        val uvDescription: String? = null,
        val uvIndex: Int? = null,
        val validTimeLocal: String? = null,
        val validTimeUtc: Long? = null,
        val visibility: Double? = null,
        val windDirection: Int? = null,
        val windDirectionCardinal: String? = null,
        val windSpeed: Int? = null,
        val wxPhraseLong: String? = null,
        val wxPhraseMedium: String? = null,
        val wxPhraseShort: String? = null
    )

    @Serializable
    data class AlertsResponse(val alerts: List<Alert>? = null)

    @Serializable
    data class Alert(
        val eventDescription: String? = null,
        val severityCode: Int? = null,
        val severity: String? = null,
        val urgency: String? = null,
        val certainty: String? = null,
    )

    @Serializable
    data class DailyForecastResponse(
        @SerialName("daypart")
        val dayPart: @Serializable(ObjectListSerializer::class) List<DailyForecastDayPart> = emptyList(),
    )

    @Serializable
    data class DailyForecastDayPart(
        val precipChance: Int?,
        val precipType: String?,
        val qpf: Double?,
        val qpfSnow: Double?,
        val qualifierCode: String?,
        val qualifierPhrase: String?,
        val relativeHumidity: Int?,
        val snowRange: String?,
        val temperature: Int?,
        val temperatureHeatIndex: Int?,
        val temperatureWindChill: Int?,
        val thunderCategory: String?,
        val thunderIndex: Int?,
        val uvDescription: String?,
        val uvIndex: Int?,
        val windDirection: Int?,
        val windDirectionCardinal: String?,
        val windPhrase: String?,
        val windSpeed: Int?,
        val wxPhraseLong: String?,
        val wxPhraseShort: String?,
    )

    @Serializable
    data class AirQualityResponse(val globalAirQuality: GlobalAirQuality?)

    @Serializable
    data class GlobalAirQuality(
        val airQualityCategory: String?,
        val pollutants: Map<String, Pollutant>?,
    )

    @Serializable
    data class Pollutant(
        val name: String? = null,
        val amount: Double? = null,
        val unit: String? = null,
    )

    @Serializable
    data class GeoCode(val name: String, val shortName: String, val latitude: Double, val longitude: Double)
}
