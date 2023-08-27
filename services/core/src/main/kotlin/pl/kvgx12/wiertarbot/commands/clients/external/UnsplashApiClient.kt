package pl.kvgx12.wiertarbot.commands.clients.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange("https://unsplash.com/napi")
interface UnsplashApiClient {
    @GetExchange("/search/photos")
    suspend fun searchPhotos(
        @RequestParam query: String,
        @RequestParam page: Int,
        @RequestParam("per_page") perPage: Int,
    ): SearchPhotosResponse

    @Serializable
    data class SearchPhotosResponse(
        val results: List<Result>,
        @SerialName("total_pages")
        val totalPages: Int,
    ) {
        @Serializable
        data class Result(val urls: Urls)

        @Serializable
        data class Urls(val regular: String)
    }
}
