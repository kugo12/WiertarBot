package pl.kvgx12.wiertarbot.commands

import com.google.protobuf.ByteString
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.Import
import org.springframework.context.support.GenericApplicationContext
import pl.kvgx12.wiertarbot.commands.standard.barka
import pl.kvgx12.wiertarbot.commands.standard.pastaXd
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.connector.ConnectorContextClient
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.utils.getCommand
import pl.kvgx12.wiertarbot.utils.proto.Response
import java.util.*

@Import(CommandTestConfiguration::class)
class StandardCommandsTest(context: GenericApplicationContext) : FunSpec() {
    @MockkBean
    lateinit var connectorContext: ConnectorContextClient

    lateinit var event: MessageEvent

    init {
        val prefix = context.getBean<WiertarbotProperties>().prefix

        afterTest {
            clearMocks(event, connectorContext)
        }

        beforeTest {
            event = mockk<MessageEvent>(relaxed = true)
            every { event.connectorInfo } returns connectorInfo {
                connectorType = ConnectorType.TELEGRAM
            }
        }

        context("wybierz") {
            val (_, handler) = context.getCommand("wybierz")

            test("no arguments") {
                every { event.text } returns "${prefix}wybierz"
                handler.process(event) shouldBe Response(event, text = "Brak opcji do wyboru")
            }

            test("returns correctly") {
                every { event.text } returns "${prefix}wybierz asd"
                handler.process(event) shouldBe Response(event, text = "asd")

                every { event.text } returns "${prefix}wybierz asd asd2 asd3"
                handler.process(event) shouldBe Response(event, text = "asd asd2 asd3")

                every { event.text } returns "${prefix}wybierz asd, asd2, asd3"

                val responses = listOf("asd", "asd2", "asd3")
                    .map { Response(event, text = it) }

                repeat(100) {
                    handler.process(event) shouldBeIn responses
                }
            }
        }

        test("moneta") {
            val (_, handler) = context.getCommand("moneta")
            every { event.text } returns "${prefix}moneta"

            val responses = listOf("Orzeł!", "Reszka!")
                .map { Response(event, text = it) }

            repeat(100) {
                handler.process(event) shouldBeIn responses
            }
        }

        test("kostka") {
            val (_, handler) = context.getCommand("kostka")
            every { event.text } returns "${prefix}kostka"

            val responses = (1..6)
                .map { Response(event, text = "Wyrzuciłeś $it") }

            repeat(100) {
                handler.process(event) shouldBeIn responses
            }
        }

        mapOf(
            "donate" to "https://paypal.me/kugo12\nZ góry dzięki",
            "changelog" to "https://github.com/kugo12/WiertarBot/commits/main",
            "kod" to "https://github.com/kugo12/WiertarBot",
            "sugestia" to "https://github.com/kugo12/WiertarBot/issues",
            "barka" to barka,
            "Xd" to pastaXd,
        ).forEach { (commandName, result) ->
            test(commandName) {
                val (_, handler) = context.getCommand(commandName)
                every { event.text } returns "${prefix}$commandName"

                handler.process(event) shouldBe Response(event, text = result)
            }
        }

        test("Xd - xd and xD return nothing") {
            val (_, handler) = context.getCommand("Xd")

            every { event.text } returns "${prefix}xd"
            handler.process(event) shouldBe Response(event)
            every { event.text } returns "${prefix}xD"
            handler.process(event) shouldBe Response(event)
        }

        context("miejski") {
            val (metadata, handler) = context.getCommand("miejski")

            test("no arguments") {
                every { event.text } returns "${prefix}miejski"
                handler.process(event) shouldBe Response(event, text = metadata.help)
            }

            test("not found") {
                every { event.text } returns "${prefix}miejski abcadsasdas"
                handler.process(event) shouldBe Response(event, text = "Nie znaleziono podanego słowa")
            }

            test("returns with one example") {
                every { event.text } returns "${prefix}miejski ***** ***"
                handler.process(event) shouldBe Response(
                    event,
                    text = """
                        ***** ***
                        Definicja:
                         5 gwiazdek w pierwszej części oznacza słowo j*bać, 3 gwiazdki w drugiej części oznaczają rządzącą aktualnie partię PIS
                         Tego zwrotu używa się w związku z rosnącą cenzurą${" "}

                        Przyklad/y:
                         - Mordo co myślisz o PISie?
                         - ***** ***
                    """.trimIndent(),
                )
            }

            test("returns without example") {
                every { event.text } returns "${prefix}miejski scamer"
                handler.process(event) shouldBe Response(
                    event,
                    text = """
                scamer
                Definicja:
                 oszust
                    """.trimIndent(),
                )
            }
        }

        context("kurs") {
            val (metadata, handler) = context.getCommand("kurs")
            fun invalidCurrencyResponse() = Response(event, text = "Nieprawidłowa waluta")
            fun helpResponse() = Response(event, text = metadata.help)

            test("invalid currency") {
                every { event.text } returns "${prefix}kurs abc123 pln"
                handler.process(event) shouldBe invalidCurrencyResponse()

                every { event.text } returns "${prefix}kurs pln abc123"
                handler.process(event) shouldBe invalidCurrencyResponse()
            }

            test("returns help on invalid arguments") {
                every { event.text } returns "${prefix}kurs"
                handler.process(event) shouldBe helpResponse()

                every { event.text } returns "${prefix}kurs abc"
                handler.process(event) shouldBe helpResponse()
            }

            test("returns correct value") {
                every { event.text } returns "${prefix}kurs USD usd"
                handler.process(event) shouldBe Response(event, text = "1.0 usd to 1.0000 usd")

                every { event.text } returns "${prefix}kurs usd USd 2.137345"
                handler.process(event) shouldBe Response(event, text = "2.137345 usd to 2.1373 usd")

                every { event.text } returns "${prefix}kurs usd pln 21.37"
                handler.process(event) shouldNotBeIn listOf(
                    invalidCurrencyResponse(),
                    helpResponse(),
                )
            }
        }

        context("fantano") {
            val (metadata, handler) = context.getCommand("fantano")

            test("returns help on invalid arguments") {
                every { event.text } returns "${prefix}fantano"
                handler.process(event) shouldBe Response(event, text = metadata.help)
            }

            // flaky test
            xtest("returns correct value") {
                every { event.text } returns "${prefix}fantano 123123 213123"
                handler.process(event) shouldBe Response(
                    event,
                    text = """
                Nazwa albumu: Kanye West - My Beautiful Dark Twisted Fantasy
                Treść: In celebration of hitting 2 million subscribers, I'm finally answering the requests to do a re-review of Kanye West's most celebrated album, My Beautiful Dark Twisted Fantasy.
                Ocena: 6/10
                    """.trimIndent(),
                )

                every { event.text } returns "${prefix}fantano 72 seasons"
                handler.process(event) shouldBe Response(
                    event,
                    text = """
                Nazwa albumu: Metallica - 72 Seasons
                Treść: Mehtallica.
                Ocena: 5/10
                    """.trimIndent(),
                )
            }
        }

        context("mc") {
            val (metadata, handler) = context.getCommand("mc")

            test("returns help on invalid arguments") {
                every { event.text } returns "${prefix}mc"
                handler.process(event) shouldBe Response(event, text = metadata.help)

                every { event.text } returns "${prefix}mc asd"
                handler.process(event) shouldBe Response(event, text = metadata.help)

                every { event.text } returns "${prefix}mc skin"
                handler.process(event) shouldBe Response(event, text = metadata.help)
            }

            test("returns correct value") {
                val size = 4
                val files = (1..size).map {
                    uploadedFile {
                        id = "test$it"
                        mimeType = "test$it"
                        content = ByteString.EMPTY
                    }
                }

                every { event.text } returns "${prefix}mc skin notch"

                // TODO: check if passed urls are reachable images
                coEvery { connectorContext.upload(match<List<String>> { it.size == size }) } returns files

                handler.process(event) shouldBe Response(
                    event,
                    text = "",
                    files = files,
                )
            }

            test("returns invalid nickname message") {
                every { event.text } returns "${prefix}mc skin ${UUID.randomUUID()} ${UUID.randomUUID()}"
                handler.process(event) shouldBe Response(event, text = "Podany nick nie istnieje")
            }
        }

        context("slownik") {
            val (metadata, handler) = context.getCommand("slownik")

            test("returns help on invalid arguments") {
                every { event.text } returns "${prefix}slownik"
                handler.process(event) shouldBe Response(event, text = metadata.help)
            }

            test("returns correct value") {
                every { event.text } returns "${prefix}slownik test"
                handler.process(event) shouldBe Response(
                    event,
                    text = """
                        test:
                        1.«zestaw punktowanych pytań lub zadań sprawdzających czyjąś wiedzę, inteligencję itp.; też: taki sprawdzian»
                        2.«próba, której poddaje się urządzenie, produkt, preparat itp. w celu sprawdzenia jego składu, właściwości i działania; też: to, co służy do przeprowadzenia takiej próby»
                    """.trimIndent(),
                )
            }

            test("returns invalid word message") {
                every { event.text } returns "${prefix}slownik ${UUID.randomUUID()}"
                handler.process(event) shouldBe Response(
                    event,
                    text = "Coś poszlo nie tak, jak nie użyłeś polskich liter, to dobry moment",
                )
            }
        }

        context("track") {
            val (metadata, handler) = context.getCommand("track")

            test("returns help on invalid arguments") {
                every { event.text } returns "${prefix}track"
                handler.process(event) shouldBe Response(event, text = metadata.help)
            }

            test("returns invalid tracking number message") {
                every { event.text } returns "${prefix}track ${UUID.randomUUID()} ${UUID.randomUUID()}"
                handler.process(event) shouldBe Response(
                    event,
                    text = "Nie znaleziono paczki",
                )
            }

            test("returns correct value") {
                every { event.text } returns "${prefix}track 395289328102"
                handler.process(event) shouldBe Response(
                    event,
                    text = """
                    Numer paczki: 395289328102
                    Dostarczono: tak
                    13/11/2025 9:3 - Shipment information sent to FedEx [ ]
                    13/11/2025 17:44 - Picked up [MARCON IT]
                    13/11/2025 18:10 - Shipment arriving On-Time [MARCON IT]
                    13/11/2025 18:11 - On the way [MARCON IT]
                    13/11/2025 19:37 - Left FedEx origin facility [MARCON IT]
                    13/11/2025 23:31 - Arrived at FedEx hub [SAN PIETRO MOSEZZO IT]
                    14/11/2025 1:13 - On the way [SAN PIETRO MOSEZZO IT]
                    14/11/2025 9:42 - Departed FedEx hub [SAN PIETRO MOSEZZO IT]
                    17/11/2025 19:37 - On the way [BARCELONA ES]
                    18/11/2025 6:57 - On the way [ELCHE ES]
                    18/11/2025 6:58 - At local FedEx facility [ELCHE ES]
                    18/11/2025 6:58 - Shipment arriving On-Time [ELCHE ES]
                    18/11/2025 8:11 - On FedEx vehicle for delivery [ELCHE ES]
                    18/11/2025 12:50 - Delivered [MUTXAMEL ES]
                    1/1/2100 0:0 - [Info] Waga paczki: 3.9kg. Miejsce doręczenia: MUTXAMEL, Spain. Typ przesyłki: FedEx International Economy
                    """.trimIndent(),
                )
            }
        }

        // TODO
        context("just returns") {
            listOf(
                "niedziela",
                "covid",
                "czas",
                "suchar",
            ).forEach {
                test(it) {
                    val (_, handler) = context.getCommand(it)
                    every { event.text } returns "${prefix}$it"
                    val response = handler.process(event)

                    response.shouldBeInstanceOf<Response>()
                    response.text.shouldNotBeBlank()
                }
            }
        }

        // TODO: szkaluj
    }
}
