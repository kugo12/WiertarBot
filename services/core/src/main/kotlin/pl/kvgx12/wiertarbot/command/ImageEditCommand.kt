package pl.kvgx12.wiertarbot.command

import com.google.protobuf.kotlin.toByteString
import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import pl.kvgx12.wiertarbot.command.dsl.CommandDsl
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.fileData
import pl.kvgx12.wiertarbot.utils.proto.Response
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

private const val MIME = "image/jpeg"
private const val FILENAME = "imageedit.jpg"

typealias ImageEdit<T> = suspend ImageEditCommand.ImageEditState.(T) -> T

abstract class ImageEditCommand(
    private val dsl: CommandDsl
) : CommandHandler {
    private val client = dsl.dsl.ref<WebClient>()

    inner class ImageEditState(
        private val initialMessage: MessageEvent,
    ) {
        val args get() = initialMessage.text.split(' ')

        suspend fun tryEditAndSend(event: MessageEvent): Boolean {
            val image = getImageFromAttachments(event) ?: return false

            editAndSend(this, event, image)
            return true
        }
    }

    protected abstract suspend fun edit(state: ImageEditState, image: BufferedImage): BufferedImage

    suspend fun check(event: MessageEvent): ImageEditState? {
        with(dsl) {
            event.context.fetchRepliedTo(event)
                ?.let { repliedTo ->
                    getImageFromAttachments(repliedTo)?.let { file ->
                        editAndSend(ImageEditState(event), event, file)
                        return null
                    }
                }
            Response(event, text = "Wyślij zdjęcie").send()
        }

        return ImageEditState(event)
    }

    private suspend fun editAndSend(state: ImageEditState, event: MessageEvent, file: ByteArray) {
        val content = edit(state, file)
        val fd = fileData {
            uri = FILENAME
            mimeType = MIME
            this.content = content.toByteString()
        }

        with(dsl) {
            Response(event, files = event.context.uploadRaw(fd)).send()
        }
    }

    private suspend fun getImageFromAttachments(event: MessageEvent): ByteArray? {
        val id = event.attachmentsList
            .firstOrNull { it.hasImage() }
            ?.id
            ?: return null

        val url = with(dsl) {
            event.context.fetchImageUrl(id)
        }

        return client.get()
            .uri(url)
            .retrieve()
            .awaitBodyOrNull<ByteArray>()
    }

    private fun write(image: BufferedImage): ByteArray = ByteArrayOutputStream().use {
        val tmp = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
        tmp.createGraphics().run {
            drawImage(image, 0, 0, null)
            dispose()
        }
        ImageIO.write(tmp, "jpeg", it)

        it.toByteArray()
    }

    private suspend fun edit(state: ImageEditState, file: ByteArray): ByteArray = withContext(Dispatchers.IO) {
        val image = ImageIO.read(ByteArrayImageInputStream(file))
        val output = edit(state, image)

        write(output)
    }

    companion object {
        init {
            System.setProperty("java.awt.headless", "true")
            ImageIO.setUseCache(false)
        }
    }
}
