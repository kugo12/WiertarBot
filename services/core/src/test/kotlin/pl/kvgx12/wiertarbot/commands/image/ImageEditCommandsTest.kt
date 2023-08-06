package pl.kvgx12.wiertarbot.commands.image

import com.google.protobuf.ByteString
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import org.springframework.beans.factory.getBean
import org.springframework.context.support.GenericApplicationContext
import org.springframework.test.context.ContextConfiguration
import pl.kvgx12.wiertarbot.command.CommandMetadata
import pl.kvgx12.wiertarbot.command.ImageEditCommand
import pl.kvgx12.wiertarbot.commands.CommandTestInitializer
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.attachment
import pl.kvgx12.wiertarbot.proto.imageAttachment
import pl.kvgx12.wiertarbot.proto.uploadedFile
import pl.kvgx12.wiertarbot.utils.proto.Response
import pl.kvgx12.wiertarbot.utils.proto.context

@ContextConfiguration(initializers = [CommandTestInitializer::class])
class ImageEditCommandsTest(context: GenericApplicationContext) : FunSpec(
    {
        val event = mockk<MessageEvent>()
        val prefix = context.getBean<WiertarbotProperties>().prefix
        val testImage = javaClass.classLoader.getResourceAsStream("test.jpg")!!.use {
            it.readBytes()
        }
        val uploaded = listOf(
            uploadedFile {
                id = "test-id"
                mimeType = "test/mimetype"
                content = ByteString.EMPTY
            },
        )

        afterTest {
            unmockkStatic(MessageEvent::context)
        }

        beforeTest {
            mockkStatic(MessageEvent::context)
        }

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

        afterTest {
            clearMocks(event)
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
                    coEvery { event.context.fetchRepliedTo(event) } returns null
                    coEvery { event.context.sendResponse(Response(event, text = "Wyślij zdjęcie")) } returns Unit

                    val checked = handler.check(event)

                    checked.shouldBeInstanceOf<ImageEditCommand.ImageEditState>()
                    checked shouldBeEqualToComparingFields handler.ImageEditState(event)

                    val eventWithImage = mockk<MessageEvent> {
                        val e = this

                        every { attachmentsList } returns dummyImageAttachments

                        every { this@mockk.context } returns mockk {
                            coEvery {
                                sendResponse(Response(e, files = uploaded))
                            } returns Unit
                            coEvery { uploadRaw(match { it.size == 1 }, false) } returns uploaded
                        }
                    }
                    mockkObject(handler)
                    coEvery { handler["getImageFromAttachments"](eventWithImage) } returns testImage

                    checked.tryEditAndSend(eventWithImage) shouldBe true
                    clearMocks(handler)
                }

                test("should return image - replied to flow") {
                    val eventWithImage = mockk<MessageEvent> {
                        every { attachmentsList } returns dummyImageAttachments
                    }

                    every { event.context } returns mockk {
                        coEvery {
                            sendResponse(Response(event, files = uploaded))
                        } returns Unit
                        coEvery { uploadRaw(match { it.size == 1 }, false) } returns uploaded
                        coEvery { fetchRepliedTo(event) } returns eventWithImage
                    }

                    mockkObject(handler)
                    coEvery { handler["getImageFromAttachments"](eventWithImage) } returns testImage

                    handler.check(event).shouldBeNull()

                    clearMocks(handler)
                }
            }
        }
    },
)
