package pl.kvgx12.wiertarbot.commands.image

import com.google.protobuf.ByteString
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.getBean
import org.springframework.context.support.GenericApplicationContext
import org.springframework.test.context.ContextConfiguration
import pl.kvgx12.wiertarbot.commands.CommandTestInitializer
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.connector.ConnectorContextClient
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.utils.getCommand

@ContextConfiguration(initializers = [CommandTestInitializer::class])
class RandomImageCommandsTest(context: GenericApplicationContext) : FunSpec(
    {
        val event = mockk<MessageEvent>()
        val prefix = context.getBean<WiertarbotProperties>().prefix
        val connectorContext = context.getBean<ConnectorContextClient>()

        afterTest {
            clearMocks(event, connectorContext)
        }

        beforeTest {
            every { event.connectorInfo } returns connectorInfo {
                connectorType = ConnectorType.TELEGRAM
            }
        }

        listOf(
            // APIs
            "hug",
            "wink",
            "pandka",
            "birb",
            "doggo",
            "beagle",
            "mem",
            "shiba",
            "zolw",

            // local media
            "jabol",
            "mikser",
            "audi",
            "bmw",
            "papaj",
            "konon",

            // scraping
            "frog",
            "jez",
            "cat",
        ).forEach { commandName ->
            test("command $commandName should return image") {
                val uploaded = listOf(
                    uploadedFile {
                        id = "test-id"
                        mimeType = "test/mimetype"
                        content = ByteString.EMPTY
                    },
                )
                every { event.text } returns "${prefix}$commandName"
                coEvery {
                    connectorContext.upload(any<String>(), false)
                } returns uploaded
                coEvery {
                    connectorContext.uploadRaw(any<FileData>(), false)
                } returns uploaded
                coEvery {
                    connectorContext.upload(match<List<String>> { it.size == 1 }, false)
                } returns uploaded
                coEvery {
                    connectorContext.uploadRaw(match<List<FileData>> { it.size == 1 }, false)
                } returns uploaded

                val response = context.getCommand(commandName)
                    .second
                    .process(event)

                response.shouldBeInstanceOf<Response>()
                response.filesList shouldBe uploaded
            }
        }
    },
)
