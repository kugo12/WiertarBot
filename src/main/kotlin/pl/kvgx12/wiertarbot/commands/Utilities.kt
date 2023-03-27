package pl.kvgx12.wiertarbot.commands

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.config.WiertarbotProperties
import pl.kvgx12.wiertarbot.events.Mention
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.services.CommandRegistrationService
import pl.kvgx12.wiertarbot.services.FBMessageService
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.services.help
import java.time.Duration
import kotlin.io.path.div

val utilityCommands = commands {
    command {
        name = "help"
        aliases = listOf("pomoc")
        help(
            usage = "(komenda)",
            returns = """
                aktualny prefix i lista komend
                z argumentem informacje o podanej komendzie
            """.trimIndent(),
        )

        val prefix = dsl.ref<WiertarbotProperties>().prefix
        val registrationService = dsl.provider<CommandRegistrationService>()
        val interpreter = dsl.ref<Interpreter>()

        generic { event ->
            val commands = registrationService.`object`.commands
            val args = event.text.split(' ', limit = 2)

            val text = if (args.size == 2) {
                commands[args.last().lowercase()]
                    ?.help(interpreter)
                    ?: "Nie znaleziono podanej komendy"
            } else {
                "Prefix: $prefix\nKomendy: ${commands.keys.joinToString(", ")}"
            }

            Response(event, text = text)
        }
    }

    command {
        name = "tid"
        help(returns = "id aktualnego wątku")

        generic { Response(it, text = it.threadId) }
    }

    command {
        name = "uid"
        help(usage = "oznaczenie", returns = "twoje id lub oznaczonej osoby")

        generic { Response(it, text = it.mentions.firstOrNull()?.threadId ?: it.authorId) }
    }

    command {
        name = "ile"
        help(returns = "ilość napisanych wiadomości od dodania bota do wątku")

        generic {
            val count = it.context.fetchThread(it.threadId).messageCount

            Response(
                it,
                text = "Odkąd tutaj jestem napisano tu $count wiadomości."
            )
        }
    }

    command {
        name = "uptime"
        help(returns = "czas od uruchomienia serwera")

        generic {
            val duration = Duration.ofNanos(System.nanoTime())
            val days = duration.toDays()
            val hours = duration.toHoursPart()
            val minutes = duration.toMinutesPart()

            Response(
                it,
                text = "Serwer jest uruchomiony od ${days}d ${hours}h ${minutes}m"
            )
        }
    }

    command {
        name = "see"
        help(usage = "(ilosc <= 10)", returns = "jedną lub więcej ostatnio usuniętych wiadomości w wątku")

        val fbMessageService = dsl.ref<FBMessageService>()

        generic { event ->
            val count = event.text.split(' ', limit = 2).last()
                .toIntOrNull()?.coerceIn(1, 10)
                ?: 1

            val messages = fbMessageService.getDeletedMessages(event.threadId, count)

            coroutineScope {
                messages.map { message ->
                    val mentions = message.mentions.map {
                        Mention(it.threadId, it.offset, it.length)
                    }
                    var voiceClip = false
                    val attachments = message.attachments.mapNotNull {
                        when (it.type) {
                            "ImageAttachment" -> Constants.attachmentSavePath / "${it.id}.${it.originalExtension}"
                            "VideoAttachment" -> Constants.attachmentSavePath / "${it.id}.mp4"
                            "AudioAttachment" -> {
                                voiceClip = true
                                Constants.attachmentSavePath / "${it.filename}"
                            }

                            else -> null
                        }
                    }

                    async {
                        val files = when {
                            attachments.isNotEmpty() -> event.context.upload(
                                attachments.map { it.toString() },
                                voiceClip
                            )

                            else -> null
                        }

                        Response(
                            event,
                            text = message.text,
                            mentions = mentions,
                            files = files,
                            voiceClip = voiceClip
                        ).send()
                    }
                }.awaitAll()
            }

            when {
                messages.isEmpty() -> Response(
                    event,
                    text = "Nie ma żadnych zapisanych usuniętych wiadomości w tym wątku"
                )

                else -> null
            }
        }
    }

    command {
        name = "perm"
        help {
            it.apply {
                usage("look <nazwa>")
                additionalUsage("<add/rem> <nazwa> <wl/bl> (tid=here/tid) <oznaczenia/uid>")
                returns("status lub tablica permisji")
            }
        }

        val permissionService = dsl.ref<PermissionService>()

        generic { event ->
            val args = event.text.split(' ')
            val text = when {
                args.size == 3 && args[1] == "look" -> {
                    permissionService.findByCommand(args[2])
                        ?.let { "${args[2]}:\n\nwhitelist: ${it.whitelist}\nblacklist: ${it.blacklist}" }
                        ?: "Podana permisja nie istnieje"
                }

                args.size > 4 -> {
                    var tid: String? = null
                    if (args[4].startsWith("tid="))
                        when {
                            args[4].drop(4).toIntOrNull() != null -> {
                                tid = args[4].drop(4)
                            }

                            args[4].endsWith("here") -> {
                                tid = event.threadId
                            }
                        }

                    val isBlacklist = args[3] != "wl"
                    val isAdd = args[1] == "add"
                    val uids = buildList {
                        addAll(args.drop(4))
                        event.mentions.mapTo(this) { it.threadId }
                    }

                    when (permissionService.edit(args[2], uids, isBlacklist, isAdd, tid)) {
                        true -> buildString {
                            append("Pomyślnie ")
                            append(if (isAdd) "dodano do " else "usunięto z ")
                            append(if (isBlacklist) "blacklisty" else "whitelisty")
                        }

                        false -> "Permisja nie istnieje"
                    }
                }

                else -> help!!
            }

            Response(event, text = text)
        }
    }
}
