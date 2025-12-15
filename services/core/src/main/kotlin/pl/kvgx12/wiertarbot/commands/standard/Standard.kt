package pl.kvgx12.wiertarbot.commands.standard

import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import org.springframework.beans.factory.BeanRegistrarDsl
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.generic
import pl.kvgx12.wiertarbot.command.dsl.text
import pl.kvgx12.wiertarbot.commands.clients.external.*
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.utils.proto.Response
import pl.kvgx12.wiertarbot.utils.proto.isGroup
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.div
import kotlin.io.path.readLines
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class StandardCommandsRegistrar : BeanRegistrarDsl({
    command("wybierz") {
        help(usage = "<opcje do wyboru po przecinku>", returns = "losowo wybraną opcję")

        text {
            it.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.split(',')
                ?.random()
                ?.trim()
                ?: "Brak opcji do wyboru"
        }
    }

    command("moneta") {
        help(returns = "wynik rzutu monetą")

        text { if (Random.nextBoolean()) "Orzeł!" else "Reszka!" }
    }

    command("kostka") {
        help(returns = "wynik rzutu kostką")

        text { "Wyrzuciłeś ${Random.nextInt(1, 7)}" }
    }

    command("donate") {
        help(returns = "link do pp")

        text { "https://paypal.me/kugo12\nZ góry dzięki" }
    }

    command("changelog") {
        help(returns = "link do spisu zmian")

        text { "https://github.com/kugo12/WiertarBot/commits/main" }
    }

    command("kod") {
        help(returns = "link do kodu bota")

        text { "https://github.com/kugo12/WiertarBot" }
    }

    command("sugestia") {
        help(returns = "https://github.com/kugo12/WiertarBot/issues")

        text { "https://github.com/kugo12/WiertarBot/issues" }
    }

    command("barka") {
        help(returns = "tekst barki")

        text { barka }
    }

    command("Xd", "xd") {
        help(returns = "copypaste o Xd")

        val prefixLength = dsl.bean<WiertarbotProperties>().prefix.length

        text { if (it.text.drop(prefixLength) == "Xd") pastaXd else null }
    }

    command("miejski") {
        help(usage = "<wyraz>", returns = "definicję podanego wyrazu z www.miejski.pl")

        val miejski = dsl.bean<Miejski>()

        text { event ->
            event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.let { miejski.getDefinition(it) }
                ?: help
        }
    }

    command("niedziela", "niedziele") {
        help(returns = "najbliższe niedziele handlowe")

        text { _ ->
            val now = Clock.System.now().toLocalDateTime(plZone).date
            val dates = sundays.filter { it >= now }
            val first = dates.firstOrNull()

            buildString {
                first?.let {
                    append(
                        if (it == now) {
                            "Dzisiejsza niedziela jest handlowa\n\n"
                        } else {
                            "Najbliższa niedziela handlowa: $it\n\n"
                        },
                    )
                }

                append("Kolejne niedziele handlowe: ")
                dates.drop(1).joinTo(this, separator = ", ")
            }
        }
    }

    command("covid") {
        help(returns = "aktualne informacje o covid w Polsce")

        val stats = dsl.bean<PLCovidStats>()

        text { stats.get() }
    }

    command("slownik", "słownik") {
        help(usage = "<wyraz>", returns = "definicje podanego wyrazu z sjp.pwn.pl")

        val sjpPwn = dsl.bean<SjpPwn>()

        text { event ->
            event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.let { sjpPwn.getDefinition(it) }
                ?: help
        }
    }

    command("track", "tracking") {
        help(usage = "<numer śledzenia>", returns = "status paczki")

        val aliPaczka = dsl.bean<AliPaczka>()

        text { event ->
            event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.let { aliPaczka.track(it) }
                ?: help
        }
    }

    command("mc") {
        help(usage = "<skin> <nick>", returns = "skin dla podanego nicku")

        val minecraft = dsl.bean<Minecraft>()

        generic { event ->
            val args = event.text.split(' ', limit = 3)

            var files: List<String>? = null
            val text = when (args.size) {
                3 -> {
                    val (command, nick) = args.drop(1)

                    when (command.lowercase()) {
                        "skin" -> run {
                            files = minecraft.getProfileSkinUrls(nick)

                            files?.let { "" }
                        }

                        else -> help
                    } ?: "Podany nick nie istnieje"
                }

                else -> help
            }

            Response(
                event,
                text = text,
                files = files?.let { event.context.upload(it) },
            )
        }
    }

    command("szkaluj") {
        help(usage = "(oznaczenie/random)", returns = "tekst szkalujący osobę")

        val lines by lazy {
            (Constants.commandMediaPath / "random/szkaluj.txt")
                .readLines()
                .map { it.replace("%n%", "\n").trim() }
                .filter { it.isNotBlank() }
        }

        generic { event ->
            val args = event.text.lowercase().split(' ', limit = 2)

            val uid = when {
                event.isGroup && args.getOrNull(1) == "random" -> {
                    event.context.fetchThread(event.threadId)!!
                        .participantsList.random()
                }

                event.mentionsList.isNotEmpty() -> event.mentionsList.first().threadId
                else -> event.authorId
            }

            val name = event.context.fetchThread(uid)?.name.orEmpty()

            val text: String
            val mentions = buildList {
                text = buildString {
                    lines.random()
                        .split("%on%")
                        .forEachIndexed { index, it ->
                            if (index == 0) {
                                append(it)
                            } else {
                                add(
                                    mention {
                                        threadId = uid
                                        offset = this@buildString.length
                                        length = name.length
                                    },
                                )
                                append(name, it)
                            }
                        }
                }
            }

            Response(event, text = text, mentions = mentions)
        }
    }

    command("czas") {
        help(returns = "aktualny czas oraz odliczenia")

        text {
            val now = Clock.System.now()

            buildString {
                append("Jest ", dateTimeFormat.format(now.toLocalDateTime(plZone).toJavaLocalDateTime()))

                czasTimers.forEach { (text, date) ->
                    val delta = date.atTime(zeroTime)
                        .toInstant(plZone)
                        .minus(now)

                    if (delta.isPositive()) {
                        append(
                            "\n", text, " ",
                            delta.inWholeDays, "d ",
                            delta.inWholeHours % 24, "h ",
                            delta.inWholeMinutes % 60, "min ",
                            delta.inWholeSeconds % 60, "sek",
                        )
                    }
                }
            }
        }
    }

    command("fantano") {
        help(usage = "<nazwa albumu>", returns = "ocene albumu fantano")

        val fantano = dsl.bean<Fantano>()

        text { event ->
            event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.let {
                    val review = fantano.getRate(it)

                    """
                        Nazwa albumu: ${review.title}
                        Treść: ${review.review}
                        Ocena: ${review.rate}
                    """.trimIndent()
                }
                ?: help
        }
    }

    command("kurs") {
        help(
            usage = "<z waluty> <do waluty> (ilosc=1)",
            returns = "Kurs aktualizowany dziennie",
        )

        val client = dsl.bean<CurrencyApi>()

        text { event ->
            event.text.split(' ', limit = 4)
                .let { if (it.size > 2) it else null }
                ?.let { args ->
                    val from = args[1].lowercase()
                    val to = args[2].lowercase()
                    val amount = args.getOrNull(3)?.toDoubleOrNull() ?: 1.0

                    try {
                        val value = client.convert(from, to, amount)

                        "$amount $from to ${String.format(Locale.ENGLISH, "%.4f", value)} $to"
                    } catch (_: CurrencyApi.InvalidCurrencyException) {
                        "Nieprawidłowa waluta"
                    }
                }
                ?: help
        }
    }

    command("suchar") {
        help(returns = "losowy suchar")

        val suchar = dsl.bean<Suchar>()

        text { suchar.random() }
    }
})

private val zeroTime = LocalTime(0, 0)
val plZone = TimeZone.of("Europe/Warsaw")
private val dateTimeFormat = DateTimeFormatter.ofPattern("eeee dd MMMM HH:mm ", Locale.of("pl", "PL"))

private val czasTimers = listOf(
    "Początek wakacji (23 czerwca) za" to LocalDate(2023, 6, 23),
    "Początek \"wakacji\" dla maturzystów (28 kwietnia) za" to LocalDate(2023, 4, 28),
)

val sundays = listOf(
    LocalDate(2023, 1, 29),
    LocalDate(2023, 4, 2),
    LocalDate(2023, 4, 30),
    LocalDate(2023, 6, 25),
    LocalDate(2023, 8, 27),
    LocalDate(2023, 12, 17),
    LocalDate(2023, 12, 24),
)

val pastaXd = """
    Serio, mało rzeczy mnie triggeruje tak jak to chore "Xd".
    Kombinacji x i d można używać na wiele wspaniałych sposobów.
    Coś cię śmieszy? Stawiasz "xD". Coś się bardzo śmieszy?
    Śmiało: "XD"! Coś doprowadza Cię do płaczu ze śmiechu? "XDDD" i załatwione.
    Uśmiechniesz się pod nosem? "xd". Po kłopocie.
    A co ma do tego ten bękart klawiaturowej ewolucji, potwór i zakała ludzkiej estetyki - "Xd"?
    Co to w ogóle ma wyrażać? Martwego człowieka z wywalonym jęzorem? Powiem Ci, co to znaczy.
    To znaczy, że masz w telefonie włączone zaczynanie zdań dużą literą, ale szkoda Ci klikać capsa na jedno "d" później.
    Korona z głowy spadnie? Nie sondze.
    "Xd" to symptom tego, że masz mnie, jako rozmówcę, gdzieś, bo Ci się nawet kliknąć nie chce, żeby mi wysłać poprawny emotikon.
    Szanujesz mnie? Używaj "xd", "xD", "XD", do wyboru. Nie szanujesz mnie? Okaż to.
    Wystarczy, że wstawisz to zjebane "Xd" w choć jednej wiadomości.
    Nie pozdrawiam
""".trimIndent().replace("\n", " ")

val barka = """
    Pan kiedyś stanął nad brzegiem
    Szukał ludzi gotowych pójść za Nim
    By łowić serca
    Słów Bożych prawdą.

    Ref.:
    O Panie, to Ty na mnie spojrzałeś,
    Twoje usta dziś wyrzekły me imię.
    Swoją barkę pozostawiam na brzegu,
    Razem z Tobą nowy zacznę dziś łów.

    2.
    Jestem ubogim człowiekiem,
    Moim skarbem są ręce gotowe
    Do pracy z Tobą
    I czyste serce.

    3.
    Ty, potrzebujesz mych dłoni,
    Mego serca młodego zapałem
    Mych kropli potu
    I samotności.

    4.
    Dziś wypłyniemy już razem
    Łowić serca na morzach dusz ludzkich
    Twej prawdy siecią
    I słowem życia.


    By Papież - https://www.youtube.com/watch?v=fimrULqiExA
    Z tekstem - https://www.youtube.com/watch?v=_o9mZ_DVTKA
""".trimIndent()
