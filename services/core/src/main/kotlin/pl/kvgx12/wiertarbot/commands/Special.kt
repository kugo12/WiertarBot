package pl.kvgx12.wiertarbot.commands

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.kvgx12.wiertarbot.command.SpecialCommand
import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.command.dsl.specialCommand
import pl.kvgx12.wiertarbot.command.dsl.specialCommandWithContext
import pl.kvgx12.wiertarbot.utils.proto.*
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.services.PermissionService

const val THINKING_EMOJI = "\uD83E\uDD14"
const val ANGRY_EMOJI = "\uD83D\uDE20"

val specialCommands = commands {
    specialCommandWithContext("everyone") {
        val permissionService = ref<PermissionService>()

        SpecialCommand { event ->
            if ("@everyone" in event.text &&
                event.isGroup &&
                permissionService.isAuthorized("everyone", event.threadId, event.authorId)
            ) {
                val mentions = event.context.fetchThread(event.threadId)
                    .participantsList.map {
                        mention {
                            threadId = it
                            offset = 0
                            length = 9
                        }
                    }

                Response(event, text = "@everyone", mentions = mentions).send()
            }
        }
    }

    specialCommand("thinking") {
        if (it.text == THINKING_EMOJI) {
            Response(it, text = THINKING_EMOJI).send()
        }
    }

    specialCommand("grek") {
        when (it.text.lowercase()) {
            "grek" -> {
                if (it.text == "Grek") {
                    Response(it, text = "grek*").send()
                }
                Response(it, text = "to pedał").send()
            }

            "pedał" -> Response(it, text = "sam jesteś grek").send()
            "pedał to" -> Response(it, text = "grek").send()
        }
    }

    specialCommandWithContext("1337") {
        val permissionService = ref<PermissionService>()

        SpecialCommand {
            if ("1337" in it.text) {
                val isLeet = permissionService.isAuthorized("leet", it.threadId, it.authorId)

                Response(it, text = if (isLeet) "Jesteś elitą" else "Nie jesteś elitą").send()
            }
        }
    }

    specialCommand("2137") {
        if ("2137" in it.text) {
            Response(it, text = "haha toż to papieżowa liczba").send()
        }
    }

    specialCommand("Xd") {
        if ("Xd" in it.text) it.react(ANGRY_EMOJI)
    }

    specialCommand("spierdalaj") { sam(it, "spierdalaj") }

    specialCommand("wypierdalaj") { sam(it, "wypierdalaj") }
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

    launch { Response(event, text = msg + word).send() }
    launch { event.react(ANGRY_EMOJI) }
}
