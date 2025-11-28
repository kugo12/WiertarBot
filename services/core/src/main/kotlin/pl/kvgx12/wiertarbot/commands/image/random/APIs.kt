package pl.kvgx12.wiertarbot.commands.image.random

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.web.reactive.function.client.WebClient
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.command.dsl.file
import pl.kvgx12.wiertarbot.command.dsl.generic
import pl.kvgx12.wiertarbot.commands.clients.external.ImgFlipClient
import pl.kvgx12.wiertarbot.commands.clients.external.UnsplashApiClient
import pl.kvgx12.wiertarbot.commands.clients.external.UnsplashQuery
import pl.kvgx12.wiertarbot.utils.proto.Response

val randomImageApiCommands = commands {
    randomImageCommand<Link>("hug", "z tuleniem", "https://api.some-random-api.com/animu/hug")
    randomImageCommand<Link>("wink", "z mrugnięciem", "https://api.some-random-api.com/animu/wink")
    randomImageCommand<Link>("pandka", "z pandką", "https://api.some-random-api.com/img/red_panda", "panda")
    randomImageCommand<Link>("birb", "z ptaszkiem", "https://api.some-random-api.com/img/bird")
    randomImageCommand<Link>("cat", "z kotem", "https://api.some-random-api.com/img/cat", "catto", "kot")

    randomImageCommand<Message>("doggo", "z pieskiem", "https://dog.ceo/api/breeds/image/random", "dog", "pies")
    randomImageCommand<Message>("beagle", "z pieskiem rasy beagle", "https://dog.ceo/api/breed/beagle/images/random")
    randomImageCommand<Message>("shiba", "z pieskiem rasy shiba", "https://dog.ceo/api/breed/shiba/images/random")

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

    command("zolw", "żółw") {
        help(returns = "zdjęcie z żółwikiem")

        val query = UnsplashQuery(dsl.ref<UnsplashApiClient>(), "turtle")

        file { query.randomImage() }
    }

    command("frog", "zabka", "żabka", "zaba", "żaba") {
        help(returns = "zdjęcie z żabką")

        val query = UnsplashQuery(dsl.ref<UnsplashApiClient>(), "frog")

        file { query.randomImage() }
    }

    command("jez", "jeż", "hedgehog") {
        help(returns = "zdjęcie z jeżykiem")

        val query = UnsplashQuery(dsl.ref<UnsplashApiClient>(), "hedgehog")

        file { query.randomImage() }
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
