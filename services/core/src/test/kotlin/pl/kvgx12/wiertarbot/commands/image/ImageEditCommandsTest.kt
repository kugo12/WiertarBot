package pl.kvgx12.wiertarbot.commands.image

import com.google.protobuf.ByteString
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import org.springframework.beans.factory.getBean
import org.springframework.context.annotation.Import
import org.springframework.context.support.GenericApplicationContext
import pl.kvgx12.wiertarbot.command.CommandMetadata
import pl.kvgx12.wiertarbot.command.ImageEditCommand
import pl.kvgx12.wiertarbot.commands.CommandTestConfiguration
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.connector.ConnectorContextClient
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.utils.proto.Response

@Import(CommandTestConfiguration::class)
class ImageEditCommandsTest(context: GenericApplicationContext) : FunSpec() {
    @MockkBean
    lateinit var connectorContext: ConnectorContextClient

    lateinit var event: MessageEvent

    val prefix = context.getBean<WiertarbotProperties>().prefix

    val uploaded = listOf(
        uploadedFile {
            id = "test-id"
            mimeType = "test/mimetype"
            content = ByteString.EMPTY
        },
    )
    val dummyImageAttachments = listOf(
        attachment {
            id = "test-id1"
            image = imageAttachment {
                width = 100
                height = 200
                originalExtension = "jpg"
                isAnimated = false
            }
        },
        attachment {
            id = "test-id2"
            image = imageAttachment {
                width = 100
                height = 200
                originalExtension = "jpg"
                isAnimated = false
            }
        },
    )

    init {
        println(System.getProperty("user.dir"))

        beforeTest {
            event = mockk()
            every { event.connectorInfo } returns connectorInfo {
                connectorType = ConnectorType.TELEGRAM
            }
        }

        afterTest {
            clearMocks(connectorContext, event)
        }

        listOf(
            "deepfry",
            "2021",
            "nobody",
            "wypierdalaj",
            "nobody test text",
        ).forEach { cmd ->
            context(cmd) {
                val metadata = context.getBean<CommandMetadata>(cmd.split(' ').first())
                val handler = metadata.handler as ImageEditCommand

                beforeTest {
                    every { event.text } returns "${prefix}$cmd"
                }

                test("should return image - normal flow") {
                    coEvery { connectorContext.fetchRepliedTo(event) } returns null
                    coEvery { connectorContext.sendResponse(Response(event, text = "Wyślij zdjęcie")) } returns Unit

                    val checked = handler.check(event)

                    checked.shouldBeInstanceOf<ImageEditCommand.ImageEditState>()
                    checked shouldBeEqualToComparingFields handler.ImageEditState(event)

                    val eventWithImage = mockk<MessageEvent> {
                        every { attachmentsList } returns dummyImageAttachments
                        every { connectorInfo } returns connectorInfo {
                            connectorType = ConnectorType.TELEGRAM
                        }
                    }
                    coEvery {
                        connectorContext.sendResponse(Response(eventWithImage, files = uploaded))
                    } returns Unit
                    coEvery { connectorContext.uploadRaw(any<FileData>(), false) } returns uploaded
                    mockkObject(handler)
                    coEvery { handler["getImageFromAttachments"](eventWithImage) } returns testImage

                    checked.tryEditAndSend(eventWithImage) shouldBe true
                    clearMocks(handler)
                }

                test("should return image - replied to flow") {
                    val eventWithImage = mockk<MessageEvent> {
                        every { attachmentsList } returns dummyImageAttachments
                    }

                    coEvery { connectorContext.sendResponse(Response(event, files = uploaded)) } returns Unit
                    coEvery { connectorContext.uploadRaw(any<FileData>(), false) } returns uploaded
                    coEvery { connectorContext.fetchRepliedTo(event) } returns eventWithImage

                    mockkObject(handler)
                    coEvery { handler["getImageFromAttachments"](eventWithImage) } returns testImage

                    handler.check(event).shouldBeNull()

                    clearMocks(handler)
                }
            }
        }
    }

    companion object {
        private val testImage = this::class.java.classLoader.getResourceAsStream("test.jpg")!!.use {
            it.readBytes()
        }
    }
}
