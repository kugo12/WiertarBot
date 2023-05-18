package pl.kvgx12.fbchat.requests

import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.fbchat.utils.tryAsString
import pl.kvgx12.fbchat.utils.tryGet

class FileData(
    val filename: String,
    val channel: ChannelProvider,
    val contentType: ContentType
) {
    fun toFormPart(name: String) = FormPart(
        name,
        channel,
        headers {
            append(HttpHeaders.ContentType, contentType)
            append(
                HttpHeaders.ContentDisposition,
                disposition.withParameter(
                    ContentDisposition.Parameters.FileName,
                    filename
                )
            )
        }
    )
}

private val disposition = ContentDisposition("form-data")

internal fun contentTypeToIdKey(contentType: String): String = when {
    contentType.isBlank() -> "file_id"
    contentType == "image/gif" -> "gif_id"
    else -> when (val first = contentType.substringBefore('/')) {
        "video", "image", "audio" -> "${first}_id"
        else -> "file_id"
    }
}

suspend fun Session.upload(files: List<FileData>): List<Pair<String, String>> {
    val isAudio = files.size == 1 && ContentType.Audio.Any.match(files.first().contentType)

    val response = this.payloadPost(
        "/ajax/mercury/upload.php",
        mapOf("voice_clip" to isAudio.toString()),
        files.mapIndexed { index, file ->
            file.toFormPart("upload_$index")
        }
    ).tryGet("metadata")

    val metadata = when (response) {
        is JsonArray -> response.asSequence()
        is JsonObject -> response.asSequence().map { it.value }
        else -> error("Could not find metadata")
    }.map {
        val contentType = it.tryGet("filetype").tryAsString() ?: ""
        val id = it.tryGet(contentTypeToIdKey(contentType)).tryAsString()
            ?: error("Could not find id for $response in $it contentType=$contentType")

        id to contentType
    }

    return metadata.toList()
}

suspend fun Session.upload(file: FileData) = upload(listOf(file))
