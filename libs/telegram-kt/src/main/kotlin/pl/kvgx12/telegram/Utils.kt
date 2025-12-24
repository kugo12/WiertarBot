package pl.kvgx12.telegram

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject
import pl.kvgx12.telegram.data.TResult
import pl.kvgx12.telegram.data.requests.TInputFile

val telegramApiUrl = URLBuilder("https://api.telegram.org").build()

internal suspend inline fun <reified T : Any> HttpResponse.tResult(): T =
    body<TResult<T>>().let {
        if (it.ok && it.result != null) {
            it.result
        } else {
            throw TelegramApiException(it.errorCode, it.description ?: "No description")
        }
    }

internal fun HttpRequestBuilder.bodyMultipartForm(func: FormBuilder.() -> Unit) =
    setBody(MultiPartFormDataContent(formData(func)))

internal fun <T> HttpRequestBuilder.multipartForm(serializer: KSerializer<T>, data: T, files: Map<String, TInputFile.Upload>) {
    val obj = json.encodeToJsonElement(serializer, data).jsonObject

    bodyMultipartForm {
        obj.forEach {
            append(it.key, it.value.toString().removeSurrounding("\""))
        }

        files.forEach { (fileKey, file) ->
            append(
                fileKey,
                file.fileData,
                Headers.build {
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"$fileKey\"; filename=\"${file.fileName}\"")
                    append(HttpHeaders.ContentType, "application/octet-stream")
                }
            )
        }
    }
}

internal val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

internal fun createHttpClient() = HttpClient(CIO) {
    install(Resources)
    install(HttpTimeout)

    install(ContentNegotiation) {
        json(json)
    }

    Logging {
        level = LogLevel.INFO
    }

    defaultRequest {
        url.takeFrom(telegramApiUrl)
        contentType(ContentType.Application.Json)
    }
}

internal class NestedJsonListSerializer<T : Any>(elementSerializer: KSerializer<T>) : NestedJsonSerializer<List<T>>(
    ListSerializer(elementSerializer),
)

internal open class NestedJsonSerializer<T : Any>(private val serializer: KSerializer<T>) : KSerializer<T> {
    override val descriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): T = (decoder as JsonDecoder).json.decodeFromString(
        serializer,
        decoder.decodeString(),
    )

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeString((encoder as JsonEncoder).json.encodeToString(serializer, value))
    }
}
