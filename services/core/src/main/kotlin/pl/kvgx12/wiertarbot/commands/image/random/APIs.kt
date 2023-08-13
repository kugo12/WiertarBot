package pl.kvgx12.wiertarbot.commands.image.random

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.web.reactive.function.client.WebClient
import pl.kvgx12.wiertarbot.command.dsl.*
import pl.kvgx12.wiertarbot.commands.clients.external.ImgFlipClient
import pl.kvgx12.wiertarbot.commands.clients.external.ShibeOnlineApiClient
import pl.kvgx12.wiertarbot.commands.clients.external.UnsplashApiClient
import pl.kvgx12.wiertarbot.utils.proto.Response
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

        val client = dsl.ref<ImgFlipClient>()

        generic { event ->
            val meme = client.getMemes()
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

        val client = dsl.ref<ShibeOnlineApiClient>()

        files { event ->
            val count = event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.toIntOrNull()
                ?: 1

            client.getShibes(count, urls = true, httpsUrls = true)
        }
    }

    command("zolw", "żółw") {
        help(returns = "zdjęcie z żółwikiem")

        var pages = 1000
        val client = dsl.ref<UnsplashApiClient>()

        file {
            val response = client.searchPhotos("turtle", Random.nextInt(1, pages), 1)

            pages = response.totalPages

            response.results.first().urls.regular
        }
    }
}

private inline fun <reified T : WithFileAndMessage> BeanDefinitionDsl.randomImageCommand(
    name: String,
    returns: String,
    apiUrl: String,
    vararg aliases: String,
) = command(name, *aliases) {
    help(returns = "losowe zdjęcie $returns")

    val client = dsl.ref<WebClient>()

    generic { event ->
        val response = client.get()
            .uri(apiUrl)
            .retrieve()
            .bodyToMono(T::class.java)
            .awaitSingle()

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
