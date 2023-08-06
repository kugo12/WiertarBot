package pl.kvgx12.wiertarbot.commands.image

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
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
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.Response
import pl.kvgx12.wiertarbot.utils.getCommand

@ContextConfiguration(initializers = [CommandTestInitializer::class])
class RandomImageCommandsTest(context: GenericApplicationContext) : FunSpec(
    {
        val event = mockk<MessageEvent>()
        val prefix = context.getBean<WiertarbotProperties>().prefix

        afterTest {
            clearMocks(event)
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
                    UploadedFile("test-id", "test/mimetype", ByteArray(0)),
                )
                every { event.text } returns "${prefix}$commandName"
                coEvery {
                    event.context.upload(
                        match<List<String>> { it.size == 1 },
                        false,
                    )
                } returns uploaded
                coEvery {
                    event.context.uploadRaw(
                        match { it.size == 1 },
                        false,
                    )
                } returns uploaded

                val response = context.getCommand(commandName)
                    .second
                    .process(event)

                response.shouldBeInstanceOf<Response>()
                response.shouldBeEqualToIgnoringFields(
                    Response(event, files = uploaded),
                    Response::text,
                )
            }
        }
    },
)
