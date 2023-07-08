@file:Suppress("NOTHING_TO_INLINE")

package pl.kvgx12.wiertarbot.utils

import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

inline fun <reified T : Any> Any?.tryCast(): T? = this as? T

suspend fun Path.contentType(): String? = withContext(Dispatchers.IO) { Files.probeContentType(this@contentType) }

fun HttpResponse.contentTypeOrNull() = try {
    contentType()
} catch (_: BadContentTypeFormatException) {
    null
}
