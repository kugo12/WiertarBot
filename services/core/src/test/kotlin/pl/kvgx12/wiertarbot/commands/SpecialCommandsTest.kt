package pl.kvgx12.wiertarbot.commands

import io.kotest.core.spec.style.FunSpec
import io.mockk.*
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.support.GenericApplicationContext
import org.springframework.test.context.ContextConfiguration
import pl.kvgx12.wiertarbot.command.CommandMetadata
import pl.kvgx12.wiertarbot.command.SpecialCommand
import pl.kvgx12.wiertarbot.command.dsl.specialCommandName
import pl.kvgx12.wiertarbot.connector.ConnectorContextClient
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.utils.proto.isGroup
import pl.kvgx12.wiertarbot.utils.responseTextMatcher

@ContextConfiguration(initializers = [CommandTestInitializer::class])
class SpecialCommandsTest(context: GenericApplicationContext) : FunSpec(
    {
        val commands = context.getBeansOfType<CommandMetadata>()
            .filterValues { it.handler is SpecialCommand }
        val permissionService = context.getBean<PermissionService>()
        val connectorContext = context.getBean<ConnectorContextClient>()
        val event = mockk<MessageEvent>()

        fun command(name: String) = checkNotNull(commands[specialCommandName(name)]).handler as SpecialCommand

        beforeTest {
            mockkStatic(MessageEvent::isGroup)
            coEvery { permissionService.isAuthorized(any(), any(), any()) } returns true
            every { event.connectorInfo } returns connectorInfo {
                connectorType = ConnectorType.TELEGRAM
            }
        }

        afterTest {
            clearMocks(event, permissionService, connectorContext)
            unmockkStatic(MessageEvent::isGroup)
        }

        test("Xd") {
            val command = command("Xd")

            command.test(event, "asfdadfdsf dsf sdf")
            command.test(event, "")
            command.test(event, "xD")
            command.test(event, "XD")
            command.test(event, "xd")

            coEvery { connectorContext.reactToMessage(event, ANGRY_EMOJI) } returns Unit
            command.test(
                event,
                "Xd",
                verify = { coVerify { connectorContext.reactToMessage(event, ANGRY_EMOJI) } },
            )
            command.test(
                event,
                "asdfdsfasdfXdasdasd",
                verify = { coVerify { connectorContext.reactToMessage(event, ANGRY_EMOJI) } },
            )
        }

        test("2137") {
            val command = command("2137")

            command.test(event, "")
            command.test(event, "dasdasdasdasdasds")

            val matcher = responseTextMatcher("haha toż to papieżowa liczba")
            command.testWithSend(connectorContext, event, "2137", matcher)
            command.testWithSend(connectorContext, event, "asdjhdgfsdigjub211130983489e321370hf", matcher)
        }

        test("thinking") {
            val command = command("thinking")

            command.test(event, "")
            command.test(event, "dasdasda")
            command.test(event, "${THINKING_EMOJI}a")
            command.test(event, "a$THINKING_EMOJI")
            command.testWithSend(connectorContext, event, THINKING_EMOJI, responseTextMatcher(THINKING_EMOJI))
        }

        test("1337") {
            val command = command("1337")

            command.test(event, "")
            command.test(event, "dasdasda")

            val verify = {
                verify { event.threadId }
                verify { event.authorId }
                coVerify { permissionService.isAuthorized("leet", "test-thread", "test-user") }
            }
            val youAreMatcher = responseTextMatcher("Jesteś elitą")
            coEvery { permissionService.isAuthorized("leet", "test-thread", "test-user") } returns true
            every { event.threadId } returns "test-thread"
            every { event.authorId } returns "test-user"

            command.testWithSend(connectorContext, event, "1337", youAreMatcher, verify)
            confirmVerified(permissionService)
            command.testWithSend(connectorContext, event, "adlkfldkshf1337fldfhdslk", youAreMatcher, verify)
            confirmVerified(permissionService)

            val youAreNotMatcher = responseTextMatcher("Nie jesteś elitą")
            coEvery { permissionService.isAuthorized("leet", "test-thread", "test-user") } returns false
            command.testWithSend(connectorContext, event, "1337", youAreNotMatcher, verify)
            confirmVerified(permissionService)
            command.testWithSend(connectorContext, event, "adlkfldkshf1337fldfhdslk", youAreNotMatcher, verify)
            confirmVerified(permissionService)
        }

        context("everyone") {
            val command = command("everyone")

            val verifyGroup = { verify { event.isGroup } }
            val verifyAll = {
                verify {
                    event.isGroup
                    event.threadId
                    event.authorId
                }
            }
            val mentions = listOf(
                "test-user1",
                "test-user2",
                "test-user3",
            ).map {
                mention {
                    threadId = it
                    offset = 0
                    length = 9
                }
            }

            beforeTest {
                every { event.isGroup } returns true
                every { event.threadId } returns "test-thread"
                every { event.authorId } returns "test-user"
                coEvery {
                    permissionService.isAuthorized("everyone", "test-thread", "test-user")
                } returns true
            }

            test("does nothing") {
                command.test(event, "")
                command.test(event, "dasdasda")

                every { event.isGroup } returns false
                command.test(event, "@everyone", verify = verifyGroup)
                command.test(event, "dasdasdasd@everyonedasdasdasd", verify = verifyGroup)

                every { event.isGroup } returns true
                coEvery {
                    permissionService.isAuthorized("everyone", "test-thread", "test-user")
                } returns false
                command.test(event, "@everyone", verify = verifyAll)
                command.test(event, "dasdasdasd@everyonedasdasdasd", verify = verifyAll)
            }

            test("mentions all users") {
                coEvery {
                    connectorContext.fetchThread("test-thread")
                } returns threadData {
                    this.participants += mentions.map { it.threadId }
                }

                val matcher = FunctionMatcher<Response>(
                    {
                        it.text == "@everyone" &&
                            it.mentionsList == mentions
                    },
                    Response::class,
                )

                command.testWithSend(connectorContext, event, "@everyone", matcher, verify = verifyAll)
                command.testWithSend(connectorContext, event, "dasdasdasd@everyonedasdasdasd", matcher, verify = verifyAll)
            }
        }

        listOf("wypierdalaj", "spierdalaj").forEach { word ->
            test(word) {
                val command = command(word)

                coEvery { connectorContext.reactToMessage(event, ANGRY_EMOJI) } returns Unit
                val verify = { coVerify { connectorContext.reactToMessage(event, ANGRY_EMOJI) } }

                command.test(event, "")
                command.test(event, "asdasdasdas")
                command.test(event, "sam")
                command.test(event, "sam")

                val sam = responseTextMatcher("sam $word")
                command.testWithSend(connectorContext, event, word, sam, verify)
                command.testWithSend(connectorContext, event, "dalkfdshjfklj${word}dasdjkasnd", sam, verify)
                command.testWithSend(connectorContext, event, "asam $word", sam, verify)
                command.testWithSend(connectorContext, event, "sam ${word}w", sam, verify)
                command.testWithSend(connectorContext, event, " sam $word", sam, verify)
                command.testWithSend(connectorContext, event, "sam $word ", sam, verify)

                command.testWithSend(connectorContext, event, "sam $word", responseTextMatcher("sam sam $word"), verify)
                command.testWithSend(
                    connectorContext,
                    event,
                    "sam sam sam $word",
                    responseTextMatcher("sam sam sam sam $word"),
                    verify,
                )
            }
        }
    },
)

private suspend inline fun SpecialCommand.test(
    event: MessageEvent,
    text: String,
    verify: () -> Unit = {},
) {
    every { event.text } returns text
    process(event)
    verify {
        event.text
    }
    verify(atLeast = 0) { event.connectorInfo }
    verify()
    confirmVerified(event)
}

private suspend inline fun SpecialCommand.testWithSend(
    connectorContext: ConnectorContextClient,
    event: MessageEvent,
    text: String,
    matcher: Matcher<Response>,
    verify: () -> Unit = {},
) {
    coEvery { connectorContext.sendResponse(match(matcher)) } returns Unit
    test(
        event,
        text,
        verify = {
            coVerify { connectorContext.sendResponse(match(matcher)) }
            verify()
        },
    )
}
