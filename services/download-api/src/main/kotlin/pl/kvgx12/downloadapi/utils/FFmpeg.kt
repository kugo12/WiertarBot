package pl.kvgx12.downloadapi.utils

import com.github.kokorin.jaffree.ffmpeg.FFmpeg
import com.github.kokorin.jaffree.ffmpeg.UrlInput
import com.github.kokorin.jaffree.ffmpeg.UrlOutput
import kotlinx.coroutines.future.await
import java.io.File

suspend inline fun ffmpeg(crossinline func: FFmpeg.() -> Unit) = io {
    FFmpeg.atPath()
        .apply(func)
        .executeAsync().toCompletableFuture().await()
}

inline var FFmpeg.overwriteOutput: Boolean
    @Deprecated("Not implemented", level = DeprecationLevel.HIDDEN)
    get() = throw UnsupportedOperationException()
    set(value) {
        setOverwriteOutput(value)
    }

fun FFmpeg.addInput(file: File) = addInput(UrlInput.fromPath(file.toPath()))
fun FFmpeg.addOutput(file: File) = addOutput(UrlOutput.toPath(file.toPath()))
fun FFmpeg.copyAudio() = addArguments("-c:a", "copy")
fun FFmpeg.copyVideo() = addArguments("-c:v", "copy")
