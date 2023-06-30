@file:Suppress("NOTHING_TO_INLINE")

package pl.kvgx12.wiertarbot.utils

import io.ktor.client.statement.*
import io.ktor.http.*
import jep.Jep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.lang.annotations.Language
import java.nio.file.Files
import java.nio.file.Path

inline fun <reified T : Any> Any?.tryCast(): T? = this as? T

inline fun Jep.execute(
    @Language("python") code: String,
) = exec(code)

suspend fun Path.contentType() = withContext(Dispatchers.IO) { Files.probeContentType(this@contentType) }

fun HttpResponse.contentTypeOrNull() = try {
    contentType()
} catch (e: BadContentTypeFormatException) {
    null
}
