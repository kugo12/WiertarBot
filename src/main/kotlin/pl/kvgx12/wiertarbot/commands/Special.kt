package pl.kvgx12.wiertarbot.commands

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import pl.kvgx12.wiertarbot.command.SpecialCommand
import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.command.specialCommand
import pl.kvgx12.wiertarbot.command.specialCommandWithContext
import pl.kvgx12.wiertarbot.events.Mention
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.services.PermissionService

private const val THINKING_EMOJI = "\uD83E\uDD14"
private const val ANGRY_EMOJI = "\uD83D\uDE20"

val specialCommands = commands {
    specialCommandWithContext {
        val permissionService = ref<PermissionService>()

        SpecialCommand {
            if ("@everyone" in it.text
                && it.isGroup
                && permissionService.isAuthorized("everyone", it.threadId, it.authorId)
            ) {
                val mentions = it.context.fetchThread(it.threadId).participants
                    .map { Mention(it, 0, 9) }

                Response(it, text = "@everyone", mentions = mentions).send()
            }
        }
    }

    specialCommand {
        if (it.text == THINKING_EMOJI)
            Response(it, text = THINKING_EMOJI).send()
    }

    specialCommand {
        when (it.text.lowercase()) {
            "grek" -> {
                if (it.text == "Grek")
                    Response(it, text = "grek*").send()
                Response(it, text = "to pedał").send()
            }

            "pedał" -> Response(it, text = "sam jesteś grek").send()
            "pedał to" -> Response(it, text = "grek").send()
        }
    }

    specialCommandWithContext {
        val permissionService = ref<PermissionService>()

        SpecialCommand {
            if ("1337" in it.text) {
                val isLeet = permissionService.isAuthorized("leet", it.threadId, it.authorId)

                Response(it, text = if (isLeet) "Jesteś elitą" else "Nie jesteś elitą").send()
            }
        }
    }

    specialCommand {
        if ("2137" in it.text)
            Response(it, text = "haha toż to papieżowa liczba").send()
    }

    specialCommand {
        if ("Xd" in it.text) it.react(ANGRY_EMOJI)
    }

    specialCommand { sam(it, "spierdalaj") }

    specialCommand { sam(it, "wypierdalaj") }
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
