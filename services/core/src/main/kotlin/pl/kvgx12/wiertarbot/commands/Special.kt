package pl.kvgx12.wiertarbot.commands

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.BeanRegistrarDsl
import pl.kvgx12.wiertarbot.command.dsl.CommandDsl
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.special
import pl.kvgx12.wiertarbot.command.dsl.specialCommandName
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.utils.proto.Response
import pl.kvgx12.wiertarbot.utils.proto.isGroup

const val THINKING_EMOJI = "\uD83E\uDD14"
const val ANGRY_EMOJI = "\uD83D\uDE20"

private val String.special get() = specialCommandName(this)

class SpecialCommandsRegistrar : BeanRegistrarDsl({
    command("everyone".special) {
        val permissionService = dsl.bean<PermissionService>()

        special { event ->
            if ("@everyone" in event.text &&
                event.isGroup &&
                permissionService.isAuthorized("everyone", event.threadId, event.authorId)
            ) {
                val mentions = event.context.fetchThread(event.threadId)!!
                    .participantsList.map {
                        mention {
                            threadId = it.id
                            offset = 0
                            length = 9
                        }
                    }

                Response(event, text = "@everyone", mentions = mentions).send()
            }
        }
    }

    command("thinking".special) {
        special {
            if (it.text == THINKING_EMOJI) {
                Response(it, text = THINKING_EMOJI).send()
            }
        }
    }

    command("grek".special) {
        special {
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
    }

    command("1337".special) {
        val permissionService = dsl.bean<PermissionService>()

        special {
            if ("1337" in it.text) {
                val isLeet = permissionService.isAuthorized("leet", it.threadId, it.authorId)

                Response(it, text = if (isLeet) "Jesteś elitą" else "Nie jesteś elitą").send()
            }
        }
    }

    command("2137".special) {
        special {
            if ("2137" in it.text) {
                Response(it, text = "haha toż to papieżowa liczba").send()
            }
        }
    }

    command("Xd".special) {
        special {
            if ("Xd" in it.text) it.context.reactToMessage(it, ANGRY_EMOJI)
        }
    }

    command("spierdalaj".special) {
        special {
            sam(it, "spierdalaj")
        }
    }

    command("wypierdalaj".special) {
        special {
            sam(it, "wypierdalaj")
        }
    }
})

private suspend fun CommandDsl.sam(event: MessageEvent, word: String) = coroutineScope {
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
    launch { event.context.reactToMessage(event, ANGRY_EMOJI) }
}
