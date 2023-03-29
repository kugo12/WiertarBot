package pl.kvgx12.wiertarbot.commands.standard

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import it.skrape.core.htmlDocument
import kotlinx.datetime.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.commands.modules.Fantano
import pl.kvgx12.wiertarbot.commands.modules.TheForexAPI
import pl.kvgx12.wiertarbot.config.WiertarbotProperties
import pl.kvgx12.wiertarbot.events.Mention
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.utils.appendElement
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.div
import kotlin.io.path.readLines
import kotlin.random.Random


val standardCommands = commands {
    command {
        name = "wybierz"
        help(usage = "<opcje do wyboru po przecinku>", returns = "losowo wybraną opcję")

        generic {
            val text = it.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.split(',')
                ?.random()
                ?: "Brak opcji do wyboru"

            Response(it, text = text)
        }
    }

    command {
        name = "moneta"
        help(returns = "wynik rzutu monetą")

        generic { Response(it, text = if (Random.nextBoolean()) "Orzeł!" else "Reszka!") }
    }

    command {
        name = "kostka"
        help(returns = "wynik rzutu kostką")

        generic { Response(it, text = "Wyrzuciłeś ${Random.nextInt(1, 7)}") }
    }

    command {
        name = "donate"
        help(returns = "link do pp")

        generic { Response(it, text = "https://paypal.me/kugo12\nZ góry dzięki") }
    }

    command {
        name = "changelog"
        help(returns = "link do spisu zmian")

        generic { Response(it, text = "https://github.com/kugo12/WiertarBot/commits/main") }
    }

    command {
        name = "kod"
        help(returns = "link do kodu bota")

        generic { Response(it, text = "https://github.com/kugo12/WiertarBot") }
    }

    command {
        name = "sugestia"
        help(returns = "https://github.com/kugo12/WiertarBot/issues")

        generic { Response(it, text = "https://github.com/kugo12/WiertarBot/issues") }
    }

    command {
        name = "barka"
        help(returns = "tekst barki")

        generic { Response(it, text = barka) }
    }

    command {
        name = "Xd"
        aliases = listOf("xd")
        help(returns = "copypaste o Xd")

        val prefixLength = dsl.ref<WiertarbotProperties>().prefix.length

        generic { if (it.text.drop(prefixLength) == "Xd") Response(it, text = pastaXd) else null }
    }

    command {
        name = "miejski"
        help(usage = "<wyraz>", returns = "definicję podanego wyrazu z www.miejski.pl")

        val baseUrl = Url("https://www.miejski.pl")

        generic { event ->
            val text = event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.let {
                    val phrase = it.lowercase()
                    val url = URLBuilder(baseUrl)
                        .appendPathSegments("slowo-${phrase.replace(' ', '+')}")
                        .build()

                    val response = client.get(url)

                    if (response.status == HttpStatusCode.NotFound) {
                        "Nie znaleziono podanego słowa"
                    } else buildString {
                        append(phrase)
                        append('\n')

                        htmlDocument(response.bodyAsText()) {
                            findFirst("main p") {
                                append("Definicja:\n")
                                appendElement(this)
                            }
                            findFirst("main blockquote") {
                                if (text.isNotEmpty()) {
                                    append("\n\nPrzyklad/y:\n")
                                    appendElement(this)
                                }
                            }
                        }
                    }
                }
                ?: help

            Response(event, text = text)
        }
    }

    command {
        name = "niedziela"
        aliases = listOf("niedziele")
        help(returns = "najbliższe niedziele handlowe")

        generic { event ->
            val now = Clock.System.now().toLocalDateTime(plZone).date
            val dates = sundays.filter { it >= now }
            val first = dates.firstOrNull()

            val text = buildString {
                first?.let {
                    append(
                        if (it == now) "Dzisiejsza niedziela jest handlowa\n\n"
                        else "Najbliższa niedziela handlowa: $it\n\n"
                    )
                }

                append("Kolejne niedziele handlowe: ")
                dates.drop(1).joinTo(this, separator = ", ")
            }

            Response(event, text = text)
        }
    }

    command {
        name = "covid"
        help(returns = "aktualne informacje o covid w Polsce")

        generic { event ->
            val data =
                client.get("https://services-eu1.arcgis.com/zk7YlClTgerl62BY/arcgis/rest/services/global_corona_actual_widok3/FeatureServer/0/query?f=json&cacheHint=true&resultOffset=0&resultRecordCount=1&where=1%3D1&outFields=*")
                    .body<CovidResponse>()
                    .features.first().attributes

            val text = """
                Statystyki COVID19 w Polsce na ${data.date}:
                Dziennie:
                ${data.dailyInfections} zakażonych
                ${data.dailyDeaths} zgonów
                ${data.dailyRecovered} ozdrowieńców
                ${data.dailyTests} testów
                ${data.dailyPositive} testów pozytywnych
                ${data.quarantine} osób na kwarantannie aktualnie
                
                Ogółem:
                ${data.totalInfections} zakażonych
                ${data.totalRecovered} ozdrowieńców
                ${data.totalDeaths} zgonów
            """.trimIndent()

            Response(event, text = text)
        }
    }

    command {
        name = "slownik"
        aliases = listOf("słownik")
        help(usage = "<wyraz>", returns = "definicje podanego wyrazu z sjp.pwn.pl")

        val baseUrl = Url("https://sjp.pwn.pl/slowniki")

        generic { event ->
            val text = event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.let {
                    val phrase = it.lowercase()
                    val url = URLBuilder(baseUrl)
                        .appendPathSegments(phrase.replace(' ', '-'))
                        .build()

                    val body = client.get(url).bodyAsText()

                    buildString {
                        htmlDocument(body) {
                            relaxed = true
                            findAll(".entry-body .ribbon-element") {
                                if (isEmpty()) {
                                    append("Coś poszlo nie tak, jak nie użyłeś polskich liter, to dobry moment")
                                } else {
                                    forEach {
                                        appendElement(it)
                                        append('\n')
                                    }
                                }
                            }
                        }
                    }
                }
                ?: help

            Response(event, text = text)
        }
    }

    command {
        name = "track"
        aliases = listOf("tracking")
        help(usage = "<numer śledzenia>", returns = "status paczki")

        generic { event ->
            val text = event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.let {
                    val response = client.post("https://api.alipaczka.pl/track/$it/") {
                        contentType(ContentType.Application.Json)
                        setBody(AliPaczkaRequest())
                    }.body<AliPaczkaResponse>()

                    buildString {
                        when {
                            response.error != null -> append(response.error)

                            else -> {
                                append(
                                    "Numer paczki: ", it,
                                    "\nDostarczono: ", if (response.isDelivered == true) "tak" else "nie",
                                )
                                response.entries?.forEach {
                                    append(
                                        "\n",
                                        Instant.fromEpochSeconds(it.time.toLong())
                                            .toLocalDateTime(plZone)
                                            .run { "$dayOfMonth/$monthNumber/$year $hour:$minute" },
                                        " - ",
                                        it.status
                                    )
                                }
                            }
                        }
                    }
                }
                ?: help

            Response(event, text = text)
        }
    }

    command {
        name = "mc"
        help(usage = "<skin> <nick>", returns = "skin dla podanego nicku")

        generic { event ->
            val args = event.text.split(' ', limit = 3)

            var files: List<String>? = null
            val text = when (args.size) {
                3 -> {
                    val (command, nick) = args.drop(1)
                    val getUuid = suspend {
                        runCatching {
                            client.get("https://api.mojang.com/users/profiles/minecraft/$nick")
                                .body<MojangUserResponse>()
                                .id
                        }.getOrNull()
                    }

                    when (command.lowercase()) {
                        "skin" -> getUuid()?.let {
                            files = listOf(
                                "https://crafatar.com/skins/$it.png",
                                "https://crafatar.com/renders/body/$it.png?overlay&scale=6",
                                "https://crafatar.com/avatars/$it.png",
                                "https://crafatar.com/renders/head/$it.png?overlay&scale=6",
                            )

                            ""
                        }

                        else -> help
                    } ?: "Podany nick nie istnieje"
                }

                else -> help
            }


            Response(event, text = text, files = files?.let { event.context.upload(it) })
        }
    }

    command {
        name = "szkaluj"
        help(usage = "(oznaczenie/random)", returns = "tekst szkalujący osobę")

        val lines by lazy {
            (Constants.commandMediaPath / "random/szkaluj.txt")
                .readLines()
        }

        generic { event ->
            val args = event.text.lowercase().split(' ', limit = 2)

            val uid = when {
                event.isGroup && args.getOrNull(1) == "random" -> {
                    event.context.fetchThread(event.threadId)
                        .participants.random()
                }

                event.mentions.isNotEmpty() -> event.mentions.first().threadId
                else -> event.authorId
            }

            val name = event.context.fetchThread(uid).name

            val text: String
            val mentions = buildList {
                text = lines.random()
                    .replace("%n%", "\n")
                    .split("%on%")
                    .fold("") { acc, next ->
                        add(Mention(uid, acc.length, name.length))

                        "$acc$name$next"
                    }
            }

            Response(event, text = text, mentions = mentions)
        }
    }

    command {
        name = "czas"
        help(returns = "aktualny czas oraz odliczenia")

        generic { event ->
            val now = Clock.System.now()

            val text = buildString {
                append("Jest ", dateTimeFormat.format(now.toLocalDateTime(plZone).toJavaLocalDateTime()))

                czasTimers.forEach { (text, date) ->
                    val delta = date.atTime(zeroTime)
                        .toInstant(plZone)
                        .minus(now)

                    if (delta.isPositive())
                        append(
                            "\n", text, " ",
                            delta.inWholeDays, "d ",
                            delta.inWholeHours % 24, "h ",
                            delta.inWholeMinutes % 60, "min ",
                            delta.inWholeSeconds % 60, "sek"
                        )
                }
            }

            Response(event, text = text)
        }
    }

// TODO
//    command {
//        name = "tts"
//        help(usage = "(lang=kod) <tekst>", returns = "wiadomość głosową text-to-speech")
//
//        generic { event ->
//            val args = event.text.split(' ', limit = 3)
//            val lang = args.getOrNull(1)?.let {
//                if (it.startsWith("lang="))
//                    it.drop(5)
//                else null
//            }
//
//            if (args.size < 2 || (lang != null && args.size < 3))
//                return@generic Response(event, text = help)
//
//            val content = TextToSpeechClient.create().use { textToSpeechClient ->
//                val input = SynthesisInput.newBuilder().run {
//                    text = args.drop(if (lang != null) 2 else 1).joinToString(" ")
//
//                    build()
//                }
//
//                val voice = VoiceSelectionParams.newBuilder().run {
//                    languageCode = lang ?: "pl-PL"
//                    ssmlGender = SsmlVoiceGender.NEUTRAL
//
//                    build()
//                }
//
//
//                val audioConfig = AudioConfig.newBuilder()
//                    .setAudioEncoding(AudioEncoding.MP3)
//                    .build()
//
//                textToSpeechClient.synthesizeSpeech(input, voice, audioConfig)
//                    .audioContent.toByteArray()
//            }
//
//            val file = event.context.uploadRaw(
//                listOf(FileData("tts.mp3", content, "audio/mp3")),
//                true
//            )
//
//            Response(event, files = file)
//        }
//    }

// TODO
//    @MessageEventDispatcher.register(aliases=['tłumacz'])
//    async def tlumacz(event: MessageEvent) -> IResponse:
//        """
//        Użycie:
//            {command} <docelowy język> <tekst>
//        Zwraca:
//            przetłumaczony tekst
//        Informacje:
//        """
//
//        msg = tlumacz.__doc__
//
//        args = event.getText().split(' ', 2)
//        if len(args) == 3:
//            try:
//                t = await translator.translate(args[2], dest=args[1])
//                msg = t.text
//            except ValueError:
//                msg = 'Zły docelowy język'
//
//        return response(event, text=msg)

    command {
        name = "fantano"
        help(usage = "<nazwa albumu>", returns = "ocene albumu fantano")

        generic { event ->
            val text = event.text.split(' ', limit = 2)
                .getOrNull(1)
                ?.let {
                    val review = Fantano.getRate(it)

                    """
                        Nazwa albumu: ${review.title}
                        Treść: ${review.review}
                        Ocena: ${review.rate}
                    """.trimIndent()
                }
                ?: help

            Response(event, text = text)
        }
    }

    command {
        name = "kurs"
        help(
            usage = "<z waluty> <do waluty> (ilosc=1)",
            returns = "Kurs aktualizowany dziennie z europejskiego banku centralnego"
        )

        generic { event ->
            val text = event.text.split(' ', limit = 4)
                .let { if (it.size > 2) it else null }
                ?.let {
                    val from = it[1].uppercase()
                    val to = it[2].uppercase()
                    val amount = it.getOrNull(3)?.toDoubleOrNull() ?: 1.0

                    runCatching {
                        TheForexAPI.convert(from, to, amount)
                    }.fold(
                        onSuccess = { "$amount $from to ${String.format("%.4f", it)} $to" },
                        onFailure = { "Nieprawidłowa waluta"; throw it }
                    )
                }
                ?: help

            Response(event, text = text)
        }
    }
}

private val zeroTime = LocalTime(0, 0)
private val plZone = kotlinx.datetime.TimeZone.of("Europe/Warsaw")
private val dateTimeFormat = DateTimeFormatter.ofPattern("eeee dd MMMM HH:mm ", Locale("pl", "PL"))

private val czasTimers = listOf(
    "Początek wakacji (23 czerwca) za" to LocalDate(2023, 6, 23),
    "Początek \"wakacji\" dla maturzystów (28 kwietnia) za" to LocalDate(2023, 4, 28),
)

@Serializable
private data class MojangUserResponse(val id: String)

@Serializable
private data class AliPaczkaRequest(
    val uid: String = "2222",
    val ver: String = "22",
)

@Serializable
private data class AliPaczkaResponse(
    @SerialName("DataEntry")
    val entries: List<Entry>? = null,
    val isDelivered: Boolean? = null,
    val error: String? = null,
) {
    @Serializable
    data class Entry(
        val time: String,
        val status: String,
    )
}

@Serializable
private data class CovidResponse(
    val features: List<Feature>
) {
    @Serializable
    data class Feature(val attributes: Attributes)

    @Serializable
    data class Attributes(
        @SerialName("DATA_SHOW") val date: String,
        @SerialName("ZAKAZENIA_DZIENNE") val dailyInfections: Int,
        @SerialName("ZGONY_DZIENNE") val dailyDeaths: Int,
        @SerialName("LICZBA_OZDROWIENCOW") val dailyRecovered: Int,
        @SerialName("TESTY") val dailyTests: Int,
        @SerialName("TESTY_POZYTYWNE") val dailyPositive: Int,
        @SerialName("KWARANTANNA") val quarantine: Int,
        @SerialName("LICZBA_ZAKAZEN") val totalInfections: Int,
        @SerialName("WSZYSCY_OZDROWIENCY") val totalRecovered: Int,
        @SerialName("LICZBA_ZGONOW") val totalDeaths: Int,
    )
}

private val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
        })
    }
}

val sundays = listOf(
    LocalDate(2023, 1, 29),
    LocalDate(2023, 4, 2),
    LocalDate(2023, 4, 30),
    LocalDate(2023, 6, 25),
    LocalDate(2023, 8, 27),
    LocalDate(2023, 12, 17),
    LocalDate(2023, 12, 24),
)

const val pastaXd =
    "Serio, mało rzeczy mnie triggeruje tak jak to chore \"Xd\". Kombinacji x i d można używać na wiele wspaniałych sposobów. Coś cię śmieszy? Stawiasz \"xD\". Coś się bardzo śmieszy? Śmiało: \"XD\"! Coś doprowadza Cię do płaczu ze śmiechu? \"XDDD\" i załatwione. Uśmiechniesz się pod nosem? \"xd\". Po kłopocie. A co ma do tego ten bękart klawiaturowej ewolucji, potwór i zakała ludzkiej estetyki - \"Xd\"? Co to w ogóle ma wyrażać? Martwego człowieka z wywalonym jęzorem? Powiem Ci, co to znaczy. To znaczy, że masz w telefonie włączone zaczynanie zdań dużą literą, ale szkoda Ci klikać capsa na jedno \"d\" później. Korona z głowy spadnie? Nie sondze. \"Xd\" to symptom tego, że masz mnie, jako rozmówcę, gdzieś, bo Ci się nawet kliknąć nie chce, żeby mi wysłać poprawny emotikon. Szanujesz mnie? Używaj \"xd\", \"xD\", \"XD\", do wyboru. Nie szanujesz mnie? Okaż to. Wystarczy, że wstawisz to zjebane \"Xd\" w choć jednej wiadomości. Nie pozdrawiam"
const val barka = """Pan kiedyś stanął nad brzegiem
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
Z tekstem - https://www.youtube.com/watch?v=_o9mZ_DVTKA"""
