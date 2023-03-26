@file:Suppress("NOTHING_TO_INLINE")

package pl.kvgx12.wiertarbot.utils

import com.sksamuel.scrimage.ImmutableImage
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ConvolveOp
import java.awt.image.Kernel

object ImageUtils {
    fun stackVertically(vararg images: ImmutableImage): ImmutableImage {
        val maxWidth = images.maxOf { it.width }
        val scaledImages = images.map { it.scaleToWidth(maxWidth) }
        val totalHeight = scaledImages.sumOf { it.height }

        var topOffset = 0
        return scaledImages.fold(ImmutableImage.create(maxWidth, totalHeight)) { acc, image ->
            acc.overlay(image, 0, topOffset).also {
                topOffset += image.height
            }
        }
    }

    fun immutableImageText(font: Font, text: String, width: Int, height: Int) =
        BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
            .drawText(font, text)
            .toImmutableImage()

    fun colorizeLut(start: Color, end: Color): Array<IntArray> {
        val r = IntArray(256)
        val g = IntArray(256)
        val b = IntArray(256)

        val rOffset = (end.red - start.red).toFloat() / 256f
        val gOffset = (end.green - start.green).toFloat() / 256f
        val bOffset = (end.blue - start.blue).toFloat() / 256f

        for (it in 0 until 256) {
            r[it] = (start.red + it * rOffset).toInt()
            g[it] = (start.green + it * gOffset).toInt()
            b[it] = (start.blue + it * bOffset).toInt()
        }

        return arrayOf(r, g, b)
    }

}

fun Font.getTextDimensions(text: String): Pair<Int, Int> {
    val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.font = this

    val metrics = graphics.fontMetrics
    val width = metrics.stringWidth(text)
    val height = metrics.height * text.count { it == '\n' }
    graphics.dispose()

    return width to height
}

inline fun BufferedImage.toImmutableImage() = ImmutableImage.fromAwt(this)

fun BufferedImage.drawText(font: Font, text: String) = this.also {
    createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
        setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        this.font = font
        color = Color.BLACK

        drawString(text, 0, fontMetrics.ascent)
        dispose()
    }
}

fun Int.contrast(factor: Double) = (factor * (this - 128) + 128).truncateUByte()
fun Int.brightness(factor: Double) = times(factor).truncateUByte()
fun Int.blend(other: Int, alpha: Double) = (this + (other-this)*alpha).truncateUByte()

inline fun color(red: Int, green: Int, blue: Int) = (red shl 16) or (green shl 8) or blue
inline fun BufferedImageOp.filter(src: BufferedImage): BufferedImage = filter(src, null)

fun Double.truncateUByte() = when {
    this >= 255 -> 255
    this <= 0 -> 0
    else -> toInt()
}

val sharpnessOp = ConvolveOp(
    Kernel(
        3, 3,
        floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f,
        )
    )
)