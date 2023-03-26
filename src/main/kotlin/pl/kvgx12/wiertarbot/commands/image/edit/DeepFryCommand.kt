package pl.kvgx12.wiertarbot.commands.image.edit

import pl.kvgx12.wiertarbot.command.ImageEdit
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.utils.*
import java.awt.Color
import java.awt.image.BufferedImage

val deepfryCommand = command {
    name = "deepfry"
    help {
        it.usage()
            .returns("usmażone zdjęcie")
            .info(replyInfo)
    }

    imageEdit { image ->
        val width = image.width
        val height = image.height

//    val flare1 = flare1Template.resizeToWidth(
//        (2f * flare1Template.width.toFloat() * flare1Template.width.toFloat() / width.toFloat()).toInt()
//    )
//    val flare2 = flare2Template.resizeToWidth(
//        (2f * flare2Template.width.toFloat() * flare2Template.width.toFloat() / width.toFloat()).toInt()
//    )

//    val step = 20
//    val color = Color(240, 53, 36).rgb
//    val flareLocations = buildList {
//        forRange(0, width, step) { x ->
//            forRange(0, height, step) { y ->
//
//                if (image.getRGB(x, y) == color)
//                    add(x to y)
//            }
//        }
//    }

        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        img.createGraphics().run {
            drawImage(image, 0, 0, null)
            dispose()
        }

        for (offset in 0 until width * height) {
            val x = offset % width
            val y = offset / width
            val pixel = (img.raster.getDataElements(x, y, null) as IntArray).first()

            val reduced = pixel and 0xF0F0F0

            val red = reduced.red()
            val lutIndex = red.brightness(2.0).contrast(1.5)
            val r = red.blend(deepFryLut[0][lutIndex], 0.6).contrast(2.0)
            val g = reduced.green().blend(deepFryLut[1][lutIndex], 0.6)
            val b = reduced.blue().blend(deepFryLut[2][lutIndex], 0.6)

            img.raster.setDataElements(
                x, y,
                intArrayOf(color(r, g, b))
            )
        }

        sharpnessOp.filter(img)
    }
}

val deepFryLut = ImageUtils.colorizeLut(Color(254, 0, 2), Color(255, 255, 15))

private inline fun Int.red() = shr(16) and 0xFF
private inline fun Int.green() = shr(8) and 0xFF
private inline fun Int.blue() = and(0xFF)

//inline fun forRange(start: Int, stop: Int, step: Int = 1, func: (Int) -> Unit) {
//    var it = start
//    while (it < stop) {
//        func(it)
//        it += step
//    }
//}
