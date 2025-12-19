package pl.kvgx12.wiertarbot.commands

import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContainOnlyOnce
import io.kotest.matchers.string.shouldNotContain
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.getBeanProvider
import org.springframework.context.annotation.Import
import org.springframework.context.support.GenericApplicationContext
import pl.kvgx12.wiertarbot.command.CommandMetadata
import pl.kvgx12.wiertarbot.command.SpecialCommand
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.connector.ConnectorContextClient
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.utils.getCommand
import pl.kvgx12.wiertarbot.utils.proto.Response
import pl.kvgx12.wiertarbot.utils.scopedCommandName

@Import(CommandTestConfiguration::class)
class UtilityCommandsTest(
    context: GenericApplicationContext,
    props: WiertarbotProperties,
) : FunSpec() {
    @MockkBean
    lateinit var connectorContext: ConnectorContextClient

    lateinit var event: MessageEvent


    init {
        val prefix = props.prefix

        afterTest {
            clearMocks(event, connectorContext)
        }

        beforeTest {
            event = mockk(relaxed = true)
            every { event.connectorInfo } returns connectorInfo {
                connectorType = ConnectorType.TELEGRAM
            }
        }

        test("tid") {
            val (_, handler) = context.getCommand("tid")
            every { event.threadId } returns "test-thread-id"

            handler.process(event) shouldBe Response(event, text = "test-thread-id")
        }

        context("uid") {
            val (_, handler) = context.getCommand("uid")

            test("returns author id") {
                every { event.authorId } returns "test-user-id"
                every { event.mentionsList } returns listOf()

                handler.process(event) shouldBe Response(event, text = "test-user-id")
            }

            test("returns first mentioned user") {
                every { event.mentionsList } returns listOf(
                    mention {
                        threadId = "test-user-id1"
                        offset = 0
                        length = 0
                    },
                    mention {
                        threadId = "test-user-id2"
                        offset = 0
                        length = 0
                    },
                )

                handler.process(event) shouldBe Response(event, text = "test-user-id1")
            }
        }

        test("ile") {
            val (_, handler) = context.getCommand("ile")

            every { event.threadId } returns "test-thread-id"
            coEvery { connectorContext.fetchThread("test-thread-id") } returns threadData {
                messageCount = 21
            }

            handler.process(event) shouldBe Response(event, text = "Odkąd tutaj jestem napisano tu 21 wiadomości.")
        }

        context("help") {
            val (_, handler) = context.getCommand("help")
            val allCommands = context.getBeanProvider<CommandMetadata>()
                .filter { it.handler !is SpecialCommand }
                .toList()

            fun unknownCommandResponse() = Response(event, text = "Nie znaleziono podanej komendy")

            suspend fun shouldReturnAllCommands(connectorType: ConnectorType) {
                val response = handler.process(event)
                response?.text shouldNotBe null
                response!!.text shouldContainOnlyOnce prefix
                allCommands
                    .filter { connectorType in it.availableIn }
                    .forEach { response.text shouldContainOnlyOnce it.name }
            }

            ConnectorType.entries
                .filter { it != ConnectorType.UNRECOGNIZED }
                .forEach { connectorType ->
                context("${connectorType.name} connector") {
                    beforeTest {
                        every { event.connectorInfo } returns connectorInfo {
                            this.connectorType = connectorType
                        }
                    }

                    test("returns prefix and list of all commands for $connectorType") {
                        every { event.text } returns "${prefix}help"
                        shouldReturnAllCommands(connectorType)
                    }

                    test("returns all help texts") {
                        allCommands
                            .filter { connectorType in it.availableIn }
                            .forEach {
                                every { event.text } returns "${prefix}help ${it.name}"

                                handler.process(event) shouldBe Response(event, text = it.help)
                            }
                    }

                    test("returns unknown command text") {
                        every { event.text } returns "${prefix}help asd-test-asddd"
                        handler.process(event) shouldBe unknownCommandResponse()

                        every { event.text } returns "${prefix}help asd rerf ads a sd"
                        handler.process(event) shouldBe unknownCommandResponse()
                    }

                    test("scopes correctly commands to connector type") {
                        val scopedCommands = ConnectorType.entries
                            .filter { it != connectorType && it != ConnectorType.UNRECOGNIZED }
                            .map { context.getCommand(it.scopedCommandName()).first }
                        val connectorCommand = context.getCommand(connectorType.scopedCommandName()).first

                        every { event.text } returns "${prefix}help"
                        handler.process(event).let { response ->
                            scopedCommands.forEach {
                                response?.text shouldNotContain it.name
                            }

                            response?.text shouldContainOnlyOnce connectorCommand.name
                        }

                        every { event.text } returns "${prefix}help ${connectorCommand.name}"
                        handler.process(event) shouldBe Response(event, text = connectorCommand.help)

                        scopedCommands.forEach {
                            every { event.text } returns "${prefix}help ${it.name}"
                            handler.process(event) shouldBe unknownCommandResponse()
                        }
                    }
                }
            }
        }
    }
}
