package pl.kvgx12.wiertarbot.commands

import com.google.protobuf.kotlin.toByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.web.reactive.function.client.WebClientResponseException
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.generic
import pl.kvgx12.wiertarbot.command.dsl.text
import pl.kvgx12.wiertarbot.commands.clients.internal.TTRSClient
import pl.kvgx12.wiertarbot.commands.clients.internal.TTRSClient.TTSRequest
import pl.kvgx12.wiertarbot.commands.clients.internal.TTRSClient.TranslateRequest
import pl.kvgx12.wiertarbot.proto.fileData
import pl.kvgx12.wiertarbot.utils.proto.Response

class TTRSCommandsRegistrar : BeanRegistrarDsl({
    if (env.getProperty("wiertarbot.ttrs.url") != null) {
        command("tts") {
            help(usage = "(lang=kod) <tekst>", returns = "wiadomość głosową text-to-speech")

            val client = dsl.bean<TTRSClient>()
            val languages by lazy {
                CoroutineScope(Dispatchers.IO).async {
                    client.textToSpeechLanguages()
                        .keys
                        .joinToString(", ")
                }
            }

            generic { event ->
                val args = event.text.split(' ', limit = 3)
                val lang = args.getOrNull(1)?.let {
                    if (it.startsWith("lang=")) {
                        it.drop(5)
                    } else {
                        null
                    }
                }

                if (args.size < 2 || (lang != null && args.size < 3)) {
                    return@generic Response(event, text = help)
                }

                try {
                    val response = client.textToSpeech(
                        TTSRequest(
                            text = args.drop(if (lang != null) 2 else 1).joinToString(" "),
                            lang = lang ?: "pl",
                        ),
                    )

                    val file = event.context.uploadRaw(
                        fileData {
                            uri = "tts.mp3"
                            mimeType = "audio/mp3"
                            content = response.toByteString()
                        },
                        true,
                    )

                    Response(event, files = file)
                } catch (_: WebClientResponseException.UnprocessableEntity) {
                    Response(event, text = "Podano nieprawidłowy język\nDostępne języki: ${languages.await()}")
                } catch (e: Exception) {
                    Response(event, text = "Napotkano niespodziewany błąd") // FIXME
                }
            }
        }

        command("tlumacz", "tłumacz") {
            help(usage = "<docelowy język> <tekst>", returns = "przetłumaczony tekst")

            val client = dsl.bean<TTRSClient>()

            text { event ->
                val args = event.text.split(' ').drop(1)

                if (args.size < 2) return@text help

                val destination = args.first()
                val text = args.drop(1).joinToString(" ")

                try {
                    client.translate(
                        TranslateRequest(
                            text = text,
                            source = null, // TODO
                            destination = destination,
                        ),
                    )
                } catch (_: WebClientResponseException.UnprocessableEntity) {
                    "Podano nieprawidłowy język"
                } catch (e: Exception) {
                    "Nastąpił niespodziewany błąd"
                }
            }
        }
    }
})
