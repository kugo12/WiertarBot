package pl.kvgx12.wiertarbot.commands

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.config.properties.TTRSProperties
import pl.kvgx12.wiertarbot.connector.FileData
import pl.kvgx12.wiertarbot.events.Response


val ttrsCommands = commands {
    command("tts") {
        help(usage = "(lang=kod) <tekst>", returns = "wiadomość głosową text-to-speech")

        val props = dsl.ref<TTRSProperties>()
        val languages by lazy {
            CoroutineScope(Dispatchers.IO).async {
                client.get(props.ttsLangUrl)
                    .body<Map<String, String>>()
                    .keys
                    .joinToString(", ")
            }
        }

        generic { event ->
            val args = event.text.split(' ', limit = 3)
            val lang = args.getOrNull(1)?.let {
                if (it.startsWith("lang="))
                    it.drop(5)
                else null
            }

            if (args.size < 2 || (lang != null && args.size < 3))
                return@generic Response(event, text = help)

            val response = client.post(props.ttsUrl) {
                contentType(ContentType.Application.Json)
                setBody(
                    TTSRequest(
                        text = args.drop(if (lang != null) 2 else 1).joinToString(" "),
                        lang = lang ?: "pl"
                    )
                )
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val file = event.context.uploadRaw(
                        listOf(FileData("tts.mp3", response.readBytes(), "audio/mp3")),
                        true
                    )

                    Response(event, files = file)
                }

                HttpStatusCode.UnprocessableEntity -> {
                    Response(event, text = "Podano nieprawidłowy język\nDostępne języki: ${languages.await()}")
                }

                else -> Response(event, text = "Napotkano niespodziewany błąd")  // FIXME
            }
        }
    }

    command("tlumacz", "tłumacz") {
        help(usage = "<docelowy język> <tekst>", returns = "przetłumaczony tekst")
        val props = dsl.ref<TTRSProperties>()

        text { event ->
            val args = event.text.split(' ').drop(1)

            if (args.size < 2) return@text help

            val destination = args.first()
            val text = args.drop(1).joinToString(" ")

            val response = client.post(props.translateUrl) {
                contentType(ContentType.Application.Json)
                setBody(
                    TranslateRequest(
                        text = text,
                        source = null,  // TODO
                        destination = destination,
                    )
                )
            }

            when (response.status) {
                HttpStatusCode.UnprocessableEntity -> "Podano nieprawidłowy język"
                HttpStatusCode.OK -> response.bodyAsText()
                else -> "Nastąpił niespodziewany błąd"
            }
        }
    }
}

private val client = HttpClient(CIO) {
    install(ContentNegotiation) { json() }
}

@Serializable
private data class TranslateRequest(
    val text: String,
    val source: String?,
    val destination: String
)

@Serializable
private data class TTSRequest(
    val text: String,
    val lang: String?
)
