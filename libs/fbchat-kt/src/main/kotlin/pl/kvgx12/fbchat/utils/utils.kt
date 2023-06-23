@file:Suppress("NOTHING_TO_INLINE")

package pl.kvgx12.fbchat.utils

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.skrape.core.htmlDocument
import it.skrape.selects.Doc
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration


internal suspend inline fun <T> HttpResponse.html(noinline func: Doc.() -> T) = bodyAsText().html(func)

internal inline fun <T> String.html(noinline func: Doc.() -> T) = htmlDocument(this, init = func)

internal inline val HttpResponse.locationHeader get() = headers[HttpHeaders.Location]

internal inline fun HttpRequestBuilder.bodyMultipartForm(noinline func: FormBuilder.() -> Unit) =
    setBody(MultiPartFormDataContent(formData(func)))

internal inline fun HttpRequestBuilder.bodyMultipartForm(vararg formData: FormPart<*>) =
    setBody(MultiPartFormDataContent(formData(*formData)))

internal inline fun HttpRequestBuilder.bodyForm(func: ParametersBuilder.() -> Unit) =
    setBody(FormDataContent(Parameters.build(func)))


internal inline fun stripJsonCruft(text: String): String {
    val objStart = text.indexOf('{')

    return if (objStart != -1)
        text.substring(objStart)
    else error("Could not find object in $text")
}

internal fun timerFlow(duration: Duration) = flow {
    while (true) {
        delay(duration)
        emit(Unit)
    }
}


internal fun String.indexOfOrNull(str: String, startIndex: Int = 0): Int? {
    val index = indexOf(str, startIndex)

    return if (index == -1) null else index
}

internal fun String.indexOfOrNull(str: Char, startIndex: Int = 0): Int? {
    val index = indexOf(str, startIndex)

    return if (index == -1) null else index
}
