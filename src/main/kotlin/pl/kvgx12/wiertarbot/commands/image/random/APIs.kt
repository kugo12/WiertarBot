package pl.kvgx12.wiertarbot.commands.image.random

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.config.WiertarbotProperties
import pl.kvgx12.wiertarbot.connector.FileData
import pl.kvgx12.wiertarbot.events.Response
import kotlin.random.Random

val randomImageApiCommands = commands {
    randomImageCommand<Link>("hug", "z tuleniem", "https://some-random-api.ml/animu/hug")
    randomImageCommand<Link>("wink", "z mrugnięciem", "https://some-random-api.ml/animu/wink")
    randomImageCommand<Link>("pandka", "z pandką", "https://some-random-api.ml/img/red_panda", listOf("panda"))
    randomImageCommand<Link>("birb", "z ptaszkiem", "https://some-random-api.ml/img/birb")

    randomImageCommand<Message>("doggo", "z pieskiem", "https://dog.ceo/api/breeds/image/random", listOf("dog", "pies"))
    randomImageCommand<Message>("beagle", "z pieskiem rasy beagle", "https://dog.ceo/api/breed/beagle/images/random")

    randomImageCommand<Meme>("mem", "z memem", "https://meme-api.com/gimme", listOf("meme"))

    command {
        name = "shiba"
        help(usage = "(ilość<=10)", returns = "zdjęcie/a z pieskami rasy shiba")

        generic { event ->
            val count = event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.toIntOrNull()
                ?: 1

            val urls = client.get("https://shibe.online/api/shibes?count=$count&urls=true&httpsUrls=true")
                .body<List<String>>()

            Response(event, files = event.context.upload(urls))
        }
    }

    bean {
        ref<WiertarbotProperties>().catApi.key?.let { key ->
            command {
                name = "catto"
                aliases = listOf("cat", "kot")
                help(returns = "zdjęcie z kotem")

                generic { event ->
                    val response = client.get("https://api.thecatapi.com/v1/images/search") {
                        header("x-api-key", key)
                    }.body<List<CatApiResponse>>()

                    Response(event, files = event.context.upload(response.first().url))
                }
            }
        }
        Unit
    }

    command {
        name = "zolw"
        aliases = listOf("żółw")
        help(returns = "zdjęcie z żółwikiem")

        var pages = 1000

        generic { event ->
            val response = client.get("https://unsplash.com/napi/search/photos") {
                parameter("per_page", "1")
                parameter("query", "turtle")
                parameter("page", Random.nextInt(1, pages))
            }.body<UnsplashSearchResponse>()

            pages = response.totalPages

            val url = response.results.first().urls.regular
            val imageResponse = client.get(url)

            val file = listOf(FileData(url, imageResponse.body(), imageResponse.contentType().toString()))

            Response(event, files = event.context.uploadRaw(file))
        }
    }
}

private val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

private inline fun <reified T : WithFileAndMessage> BeanDefinitionDsl.randomImageCommand(
    name: String,
    returns: String,
    apiUrl: String,
    aliases: List<String> = emptyList(),
) = command {
    this.name = name
    this.aliases = aliases
    help(returns = "losowe zdjęcie $returns")

    generic { event ->
        val response = client.get(apiUrl).body<T>()

        Response(
            event,
            text = response.message,
            files = event.context.upload(response.file)
        )
    }
}

private interface WithFileAndMessage {
    val file: String
    val message: String? get() = null
}

@Serializable
private data class Link(
    @SerialName("link")
    override val file: String
) : WithFileAndMessage

@Serializable
private data class Message(
    @SerialName("message")
    override val file: String
) : WithFileAndMessage

@Serializable
private data class Meme(
    @SerialName("title")
    override val message: String,
    @SerialName("url")
    override val file: String,
) : WithFileAndMessage

@Serializable
private data class CatApiResponse(val url: String)

@Serializable
private data class UnsplashSearchResponse(
    val results: List<Result>,
    @SerialName("total_pages")
    val totalPages: Int,
) {
    @Serializable
    data class Result(
        val urls: Urls
    )

    @Serializable
    data class Urls(
        val regular: String
    )
}
