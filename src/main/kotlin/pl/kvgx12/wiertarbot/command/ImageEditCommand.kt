package pl.kvgx12.wiertarbot.command

import com.twelvemonkeys.imageio.stream.ByteArrayImageInputStream
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.kvgx12.wiertarbot.connector.ConnectorType
import pl.kvgx12.wiertarbot.connector.FileData
import pl.kvgx12.wiertarbot.events.ImageAttachment
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.events.Response
import pl.kvgx12.wiertarbot.utils.tryCast
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

private const val mime = "image/jpeg"
private const val fileName = "imageedit.jpg"

typealias ImageEdit<T> = suspend ImageEditCommand.ImageEditState.(T) -> T


abstract class ImageEditCommand(
    override val help: String,
    override val name: String,
    override val aliases: List<String>,
    override val availableIn: EnumSet<ConnectorType>,
) : CommandData {
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
        event.context.fetchRepliedTo(event)?.let { repliedTo ->
            getImageFromAttachments(repliedTo)?.let { file ->
                editAndSend(ImageEditState(event), event, file)
                return null
            }
        }

        Response(event, text = "Wyślij zdjęcie").send()
        return ImageEditState(event)
    }

    private suspend fun editAndSend(state: ImageEditState, event: MessageEvent, file: ByteArray) {
        val f = edit(state, file)
        val file = event.context.uploadRaw(listOf(FileData(fileName, f, mime)), false)

        Response(event, files = file).send()
    }

    private suspend fun getImageFromAttachments(event: MessageEvent): ByteArray? {
        val id = event.attachments.firstOrNull()
            .tryCast<ImageAttachment>()?.id
            ?: return null

        return withContext(Dispatchers.IO) {
            val url = event.context.fetchImageUrl(id)
            val response = client.get(url)

            when (response.status) {
                HttpStatusCode.OK -> response.body<ByteArray>()
                else -> null
            }
        }
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
        val client = HttpClient()

        init {
            System.setProperty("java.awt.headless", "true")
            ImageIO.setUseCache(false)
        }
    }
}
