package pl.kvgx12.wiertarbot.commands.clients.external

import kotlinx.serialization.Serializable
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange("https://api.mojang.com")
interface MojangApiClient {
    @GetExchange("/users/profiles/minecraft/{name}")
    suspend fun getUuid(@PathVariable name: String): UuidResponse

    @Serializable
    data class UuidResponse(val id: String)
}

class Minecraft(private val client: MojangApiClient) {
    suspend fun getProfileSkinUrls(name: String): List<String>? {
        val uuid = try {
            client.getUuid(name).id
        } catch (_: WebClientResponseException.NotFound) {
            return null
        } catch (_: WebClientResponseException.BadRequest) {
            return null
        }

        return listOf(
            "https://crafatar.com/skins/$uuid.png",
            "https://crafatar.com/renders/body/$uuid.png?overlay&scale=6",
            "https://crafatar.com/avatars/$uuid.png",
            "https://crafatar.com/renders/head/$uuid.png?overlay&scale=6",
        )
    }
}
