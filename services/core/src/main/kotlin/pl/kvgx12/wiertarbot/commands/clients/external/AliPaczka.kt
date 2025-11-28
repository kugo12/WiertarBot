package pl.kvgx12.wiertarbot.commands.clients.external

import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange
import pl.kvgx12.wiertarbot.commands.clients.external.AliPaczkaClient.TrackRequest
import pl.kvgx12.wiertarbot.commands.standard.plZone

@HttpExchange("https://api.alipaczka.pl")
interface AliPaczkaClient {
    @PostExchange("/track/{id}/")
    suspend fun track(
        @PathVariable id: String,
        @RequestBody request: TrackRequest,
    ): TrackResponse

    @Serializable
    data class TrackRequest(val uid: String, val ver: String)

    @Serializable
    data class TrackResponse(
        @SerialName("DataEntry")
        val entries: List<Entry>,
        val isDelivered: Boolean,
    ) {
        @Serializable
        data class Entry(val time: Long, val status: String)
    }
}

class AliPaczka(private val client: AliPaczkaClient) {
    suspend fun track(id: String): String {
        val response = try {
            client.track(id, TrackRequest("2222", "22"))
        } catch (_: WebClientResponseException.NotFound) {
            return "Nie znaleziono paczki"
        }

        return buildString {
            append(
                "Numer paczki: ",
                id,
                "\nDostarczono: ",
                if (response.isDelivered) "tak" else "nie",
            )
            response.entries.forEach {
                append(
                    "\n",
                    Instant.fromEpochSeconds(it.time)
                        .toLocalDateTime(plZone)
                        .run { "$dayOfMonth/$monthNumber/$year $hour:$minute" },
                    " - ",
                    it.status,
                )
            }
        }
    }
}
