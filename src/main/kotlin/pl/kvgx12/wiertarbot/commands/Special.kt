package pl.kvgx12.wiertarbot.commands

import jep.python.PyObject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import pl.kvgx12.wiertarbot.config.SpecialCommandsConfiguration
import pl.kvgx12.wiertarbot.events.Mention
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.python.get
import pl.kvgx12.wiertarbot.services.PermissionService

private const val THINKING_EMOJI = "\uD83E\uDD14"
private const val ANGRY_EMOJI = "\uD83D\uDE20"

@Component
class SpecialCommands(
    specialCommands: SpecialCommandsConfiguration,
    permissionService: PermissionService,
    interpreter: Interpreter,
) {
    init {
        specialCommands.new(
            {
                if ("@everyone" in it.text
                    && it.isGroup
                    && permissionService.isAuthorized("everyone", it.threadId, it.authorId)
                ) {
                    interpreter {
                        val mentions = it.context.fetchThread(it.threadId).participants
                            .map { Mention(it, 0, 9) }

                        Response(it, text = "@everyone", mentions = mentions).send()
                    }
                }
            },
            {
                if (it.text == THINKING_EMOJI)
                    Response(it, text = THINKING_EMOJI).send()
            },
            {
                when (it.text.lowercase()) {
                    "grek" -> {
                        if (it.text == "Grek")
                            Response(it, text = "grek*").send()
                        Response(it, text = "to pedał").send()
                    }

                    "pedał" -> Response(it, text = "sam jesteś grek").send()
                    "pedał to" -> Response(it, text = "grek").send()
                }
            },
            {
                if ("1337" in it.text) {
                    val isLeet = permissionService.isAuthorized("leet", it.threadId, it.authorId)

                    Response(it, text = if (isLeet) "Jesteś elitą" else "Nie jesteś elitą").send()
                }
            },
            {
                if ("2137" in it.text)
                    Response(it, text = "haha toż to papieżowa liczba").send()
            },
            {
                if ("Xd" in it.text) it.react(ANGRY_EMOJI)
            },
            { sam(it, "spierdalaj") },
            { sam(it, "wypierdalaj") },
        )
    }

    private suspend fun sam(event: MessageEvent, word: String) = coroutineScope {
        val text = event.text.lowercase()

        if (word !in text) return@coroutineScope

        var msg = "sam "

        if (text.startsWith("sam") && text.endsWith(word)) {
            val t = text.replace(" ", "")
                .replace("sam", "")
                .replaceFirst(word, "")

            if (t.isEmpty()) {
                msg = "sam ".repeat(text.split("sam").size)
            }
        }

        val response = async { Response(event, text = msg + word).send() }
        val react = async { event.react(ANGRY_EMOJI) }

        awaitAll(response, react)
    }
}
