package pl.kvgx12.wiertarbot.commands.clients.external

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import kotlin.random.Random

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

class UnsplashQuery(
    private val client: UnsplashApiClient,
    private val query: String,
) {
    private var pages = 10

    suspend fun randomImage(): String {
        val response = client.searchPhotos(query, Random.nextInt(1, pages), 20)

        pages = response.totalPages

        return response.results.random().urls.regular
    }
}
