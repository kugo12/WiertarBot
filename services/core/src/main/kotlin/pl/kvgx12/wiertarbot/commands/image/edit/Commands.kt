package pl.kvgx12.wiertarbot.commands.image.edit

import com.sksamuel.scrimage.ImmutableImage
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.command.dsl.immutableImageEdit
import pl.kvgx12.wiertarbot.utils.ImageUtils
import pl.kvgx12.wiertarbot.utils.getTextDimensions
import java.awt.Font
import java.io.File

const val REPLY_INFO = "działa również na zdjęcia z odpowiedzi"

private fun lazyLoad(file: String, init: ImmutableImage.() -> ImmutableImage = { this }): Lazy<ImmutableImage> = lazy {
    ImmutableImage.loader().fromFile(file).run(init)
}

val wypierdalajTemplate by lazyLoad("${Constants.commandMediaPath}/templates/wypierdalaj.jpg")
val template2021 by lazyLoad("${Constants.commandMediaPath}/templates/2021.jpg")
val flare1Template by lazyLoad("${Constants.commandMediaPath}/templates/flara.png") { contrast(2.0) }
val flare2Template by lazyLoad("${Constants.commandMediaPath}/templates/flara2.png")

val font: Font by lazy {
    Font.createFont(Font.TRUETYPE_FONT, File("${Constants.commandMediaPath}/fonts/roboto/Roboto-Regular.ttf"))
        .deriveFont(44f)
}

val imageEditCommands = commands {
    deepfryCommand()

    command("2021") {
        help(
            returns = "przerobione zdjęcie z templatem 2021",
            info = REPLY_INFO,
        )

        immutableImageEdit {
            ImageUtils.stackVertically(
                template2021,
                it,
            )
        }
    }

    command("nobody") {
        help(
            usage = "(tekst)",
            returns = "przerobione zdjęcie z opcjonalnym własnym tekstem",
            info = REPLY_INFO,
        )

        immutableImageEdit {
            val text = args.drop(1)
                .joinToString(separator = " ")
                .ifEmpty { "Nobody:\n\nMe:   " }

            val (textWidth, textHeight) = font.getTextDimensions(text)
            val textImage = ImageUtils.immutableImageText(font, text, (textWidth * 1.7).toInt(), textHeight + 22)

            ImageUtils.stackVertically(textImage, it)
        }
    }

    command("wypierdalaj") {
        help(
            returns = "przerobione zdjęcie z WYPIERDALAJ",
            info = REPLY_INFO,
        )

        immutableImageEdit {
            ImageUtils.stackVertically(
                it,
                wypierdalajTemplate.scaleToHeight(it.height / 8),
            )
        }
    }
}
