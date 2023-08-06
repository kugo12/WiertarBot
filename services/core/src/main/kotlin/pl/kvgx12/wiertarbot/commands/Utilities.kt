package pl.kvgx12.wiertarbot.commands

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.command.dsl.text
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.connectors.fb.FBMessageService
import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.proto.connector.fetchThreadRequest
import pl.kvgx12.wiertarbot.proto.connector.uploadRequest
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.services.CommandRegistrationService
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.utils.proto.Response
import pl.kvgx12.wiertarbot.utils.proto.context
import pl.kvgx12.wiertarbot.utils.proto.send
import pl.kvgx12.wiertarbot.utils.proto.set
import java.time.Duration
import kotlin.io.path.div

val utilityCommands = commands {
    command("help", "pomoc") {
        help(
            usage = "(komenda)",
            returns = """
                aktualny prefix i lista komend
                z argumentem informacje o podanej komendzie
            """.trimIndent(),
        )

        val prefix = dsl.ref<WiertarbotProperties>().prefix
        val registrationService by lazy {
            dsl.ref<CommandRegistrationService>()
        }

        val lowercasedCommands by lazy {
            registrationService
                .commandsByConnector
                .mapValues { (_, value) ->
                    value.mapKeys {
                        it.key.lowercase()
                    }
                }
        }

        text { event ->
            val args = event.text.split(' ', limit = 2)

            if (args.size == 2) {
                lowercasedCommands[event.connectorInfo.connectorType]!![args.last().lowercase()]
                    ?.help
                    ?: "Nie znaleziono podanej komendy"
            } else {
                val commands = registrationService.commandsByConnector[event.connectorInfo.connectorType]!!

                "Prefix: $prefix\nKomendy: ${commands.keys.joinToString(", ")}"
            }
        }
    }

    command("tid") {
        help(returns = "id aktualnego wątku")

        text { it.threadId }
    }

    command("uid") {
        help(usage = "oznaczenie", returns = "twoje id lub oznaczonej osoby")

        text { it.mentionsList.firstOrNull()?.threadId ?: it.authorId }
    }

    command("ile") {
        help(returns = "ilość napisanych wiadomości od dodania bota do wątku")

        text {
            val count = it.context.fetchThread(
                fetchThreadRequest { threadId = it.threadId },
            ).thread.messageCount

            "Odkąd tutaj jestem napisano tu $count wiadomości."
        }
    }

    command("uptime") {
        help(returns = "czas od uruchomienia serwera")

        text {
            val duration = Duration.ofNanos(System.nanoTime())
            val days = duration.toDays()
            val hours = duration.toHoursPart()
            val minutes = duration.toMinutesPart()

            "Serwer jest uruchomiony od ${days}d ${hours}h ${minutes}m"
        }
    }

    bean {
        provider<FBMessageService>().ifAvailable { fbMessageService ->
            command("see") {
                help(usage = "(ilosc <= 10)", returns = "jedną lub więcej ostatnio usuniętych wiadomości w wątku")
                availableIn = ConnectorType.FB.set()

                text { event ->
                    val count = event.text.split(' ', limit = 2).last()
                        .toIntOrNull()?.coerceIn(1, 10)
                        ?: 1

                    val messages = fbMessageService.getDeletedMessages(event.threadId, count)

                    var isEmpty = true
                    coroutineScope {
                        messages.collect { message ->
                            isEmpty = false
                            val mentions = message.mentions.map {
                                mention {
                                    threadId = it.threadId
                                    offset = it.offset
                                    length = it.length
                                }
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

                            launch {
                                val files = when {
                                    attachments.isNotEmpty() -> event.context.upload(
                                        uploadRequest {
                                            files += attachments.map { it.toString() }
                                            this.voiceClip = voiceClip
                                        },
                                    )

                                    else -> null
                                }

                                Response(
                                    event,
                                    text = message.text,
                                    mentions = mentions,
                                    files = files?.filesList,
                                    voiceClip = voiceClip,
                                ).send()
                            }
                        }
                    }

                    when {
                        isEmpty -> "Nie ma żadnych zapisanych usuniętych wiadomości w tym wątku"
                        else -> null
                    }
                }
            }
        }
    }

    command("perm") {
        help {
            it.apply {
                usage("look <nazwa>")
                additionalUsage("<add/rem> <nazwa> <wl/bl> (tid=here/tid) <oznaczenia/uid>")
                returns("status lub tablica permisji")
            }
        }

        val permissionService = dsl.ref<PermissionService>()

        text { event ->
            val args = event.text.split(' ')

            when {
                args.size == 3 && args[1] == "look" -> {
                    permissionService.findByCommand(args[2])
                        ?.let { "${args[2]}:\n\nwhitelist: ${it.whitelist}\nblacklist: ${it.blacklist}" }
                        ?: "Podana permisja nie istnieje"
                }

                args.size > 4 -> {
                    var tid: String? = null
                    if (args[4].startsWith("tid=")) {
                        when {
                            args[4].drop(4).toIntOrNull() != null -> {
                                tid = args[4].drop(4)
                            }

                            args[4].endsWith("here") -> {
                                tid = event.threadId
                            }
                        }
                    }

                    val isBlacklist = args[3] != "wl"
                    val isAdd = args[1] == "add"
                    val uids = buildList {
                        addAll(args.drop(4))
                        event.mentionsList.mapTo(this) { it.threadId }
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
        }
    }
}
