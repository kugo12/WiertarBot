@file:Suppress("NOTHING_TO_INLINE")

package pl.kvgx12.wiertarbot.telegram

import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.content.TextContent
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

inline val Message.text get() = tryCast<ContentMessage<*>>()?.content?.tryCast<TextContent>()?.text
