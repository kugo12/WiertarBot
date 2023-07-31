package pl.kvgx12.wiertarbot.commands

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
import org.springframework.context.support.GenericApplicationContext
import org.springframework.test.context.ContextConfiguration
import pl.kvgx12.wiertarbot.commands.standard.barka
import pl.kvgx12.wiertarbot.commands.standard.pastaXd
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.connector.UploadedFile
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.utils.getCommand
import java.util.*

@ContextConfiguration(initializers = [CommandTestInitializer::class])
class StandardCommandsTest(context: GenericApplicationContext) : FunSpec(
    {
        val event = mockk<MessageEvent>()
        val prefix = context.getBean<WiertarbotProperties>().prefix

        afterTest {
            clearMocks(event)
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
            val invalidCurrencyResponse = Response(event, text = "Nieprawidłowa waluta")
            val helpResponse = Response(event, text = metadata.help)

            test("invalid currency") {
                every { event.text } returns "${prefix}kurs abc pln"
                handler.process(event) shouldBe invalidCurrencyResponse

                every { event.text } returns "${prefix}kurs pln abc"
                handler.process(event) shouldBe invalidCurrencyResponse
            }

            test("returns help on invalid arguments") {
                every { event.text } returns "${prefix}kurs"
                handler.process(event) shouldBe helpResponse

                every { event.text } returns "${prefix}kurs abc"
                handler.process(event) shouldBe helpResponse
            }

            test("returns correct value") {
                every { event.text } returns "${prefix}kurs usd usd"
                handler.process(event) shouldBe Response(event, text = "1.0 USD to 1.0000 USD")

                every { event.text } returns "${prefix}kurs usd usd 2.137345"
                handler.process(event) shouldBe Response(event, text = "2.137345 USD to 2.1373 USD")

                every { event.text } returns "${prefix}kurs usd pln 21.37"
                handler.process(event) shouldNotBeIn listOf(
                    invalidCurrencyResponse,
                    helpResponse,
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
                val files = listOf(
                    UploadedFile("test", "test", ByteArray(0)),
                    UploadedFile("test2", "test2", ByteArray(0)),
                    UploadedFile("test3", "test3", ByteArray(0)),
                    UploadedFile("test4", "test4", ByteArray(0)),
                )

                every { event.text } returns "${prefix}mc skin notch"

                // TODO: check if passed urls are reachable images
                coEvery { event.context.upload(match<List<String>> { it.size == 4 }) } returns files

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
                        test testu, teście; testów${" "}
                         stress test (bank.) stress testu, stress teście; stress testów${" "}
                         testo (muz.) teście${" "}
                         test 1. «zestaw punktowanych pytań lub zadań sprawdzających czyjąś wiedzę, inteligencję itp.; też: taki sprawdzian» 2. «próba, której poddaje się urządzenie, produkt, preparat itp. w celu sprawdzenia jego składu, właściwości i działania; też: to, co służy do przeprowadzenia takiej próby»${" "}
                        • testowy${" "}
                         test ciążowy zob. próba ciążowa.${" "}
                         testo «w oratoriach i pasjach: partia wokalna w formie recytatywu narratora objaśniającego tło akcji oraz sytuację dramatyczną»${" "}
                         test
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
                every { event.text } returns "${prefix}track 27050468080"
                handler.process(event) shouldBe Response(
                    event,
                    text = """
                    Numer paczki: 27050468080
                    Dostarczono: tak
                    18/5/2023 15:14 - nadana w DHL POP przesyłka oczekuje na odbiór przez kuriera DHL [Poznań]
                    19/5/2023 12:18 - przesyłka odebrana przez Kuriera DHL z DHL POP [Poznań]
                    19/5/2023 17:13 - Przesyłka jest obsługiwana w centrum sortowania [Poznań]
                    19/5/2023 17:13 - Przesyłka opuściła Polskę [Poznań]
                    19/5/2023 17:13 - przesyłka jest obsługiwana w centrum sortowania [Poznań]
                    19/5/2023 17:13 - przesyłka opuściła Polskę [Poznań]
                    22/5/2023 8:41 - przesyłka dotarła do oddziału [Rüdersdorf-15562]
                    22/5/2023 8:43 - przesyłka doręczona do odbiorcy [Neumark-08496]
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
    },
)
