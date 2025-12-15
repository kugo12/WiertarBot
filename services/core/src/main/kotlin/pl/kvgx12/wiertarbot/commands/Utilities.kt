package pl.kvgx12.wiertarbot.commands

import org.springframework.beans.factory.BeanRegistrarDsl
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.delegated
import pl.kvgx12.wiertarbot.command.dsl.text
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.services.CommandRegistrationService
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.utils.proto.set
import java.time.Duration

class UtilityCommandsRegistrar : BeanRegistrarDsl({
    command("help", "pomoc") {
        help(
            usage = "(komenda)",
            returns = """
                aktualny prefix i lista komend
                z argumentem informacje o podanej komendzie
            """.trimIndent(),
        )

        val prefix = dsl.bean<WiertarbotProperties>().prefix
        val registrationService by lazy {
            dsl.bean<CommandRegistrationService>()
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
            val count = it.context.fetchThread(it.threadId)
                ?.messageCount ?: 0

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

    command("see") {
        help(usage = "(ilosc <= 10)", returns = "jedną lub więcej ostatnio usuniętych wiadomości w wątku")
        availableIn = ConnectorType.FB.set()

        delegated()
    }

    command("perm") {
        help {
            it.apply {
                usage("look <nazwa>")
                additionalUsage("<add/rem> <nazwa> <wl/bl> (tid=here/tid) <oznaczenia/uid>")
                returns("status lub tablica permisji")
            }
        }

        val permissionService = dsl.bean<PermissionService>()

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
})
