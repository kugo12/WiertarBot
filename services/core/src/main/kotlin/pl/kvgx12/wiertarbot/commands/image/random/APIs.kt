package pl.kvgx12.wiertarbot.commands.image.random

import com.google.protobuf.kotlin.toByteString
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
import pl.kvgx12.wiertarbot.command.dsl.*
import pl.kvgx12.wiertarbot.utils.proto.Response
import pl.kvgx12.wiertarbot.utils.proto.context
import pl.kvgx12.wiertarbot.proto.fileData
import kotlin.random.Random

val randomImageApiCommands = commands {
    randomImageCommand<Link>("hug", "z tuleniem", "https://some-random-api.com/animu/hug")
    randomImageCommand<Link>("wink", "z mrugnięciem", "https://some-random-api.com/animu/wink")
    randomImageCommand<Link>("pandka", "z pandką", "https://some-random-api.com/img/red_panda", "panda")
    randomImageCommand<Link>("birb", "z ptaszkiem", "https://some-random-api.com/img/bird")

    randomImageCommand<Message>("doggo", "z pieskiem", "https://dog.ceo/api/breeds/image/random", "dog", "pies")
    randomImageCommand<Message>("beagle", "z pieskiem rasy beagle", "https://dog.ceo/api/breed/beagle/images/random")

    command("mem", "meme") {
        help(returns = "losowy mem")

        generic { event ->
            val meme = client.get("https://api.imgflip.com/get_memes")
                .body<ImgFlipResponse>()
                .data.memes.random()

            Response(
                event,
                text = meme.name,
                files = event.context.upload(meme.url),
            )
        }
    }

    command("shiba") {
        help(usage = "(ilość<=10)", returns = "zdjęcie/a z pieskami rasy shiba")

        files { event ->
            val count = event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.toIntOrNull()
                ?: 1

            client.get("https://shibe.online/api/shibes?count=$count&urls=true&httpsUrls=true")
                .body<List<String>>()
        }
    }

    command("zolw", "żółw") {
        help(returns = "zdjęcie z żółwikiem")

        var pages = 1000

        rawFile {
            val response = client.get("https://unsplash.com/napi/search/photos") {
                parameter("per_page", "1")
                parameter("query", "turtle")
                parameter("page", Random.nextInt(1, pages))
            }.body<UnsplashSearchResponse>()

            pages = response.totalPages

            val url = response.results.first().urls.regular
            val imageResponse = client.get(url)

            fileData {
                uri = url
                content = imageResponse.body<ByteArray>().toByteString()
                mimeType = imageResponse.contentType().toString()
            }
        }
    }
}

private val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            },
        )
    }
}

private inline fun <reified T : WithFileAndMessage> BeanDefinitionDsl.randomImageCommand(
    name: String,
    returns: String,
    apiUrl: String,
    vararg aliases: String,
) = command(name, *aliases) {
    help(returns = "losowe zdjęcie $returns")

    generic { event ->
        val response = client.get(apiUrl).body<T>()

        Response(
            event,
            text = response.message,
            files = event.context.upload(response.file),
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
    override val file: String,
) : WithFileAndMessage

@Serializable
private data class Message(
    @SerialName("message")
    override val file: String,
) : WithFileAndMessage

@Serializable
private data class ImgFlipResponse(
    val success: Boolean,
    val data: Data,
) {
    @Serializable
    data class Data(val memes: List<Meme>)

    @Serializable
    data class Meme(val name: String, val url: String)
}

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
        val urls: Urls,
    )

    @Serializable
    data class Urls(
        val regular: String,
    )
}
