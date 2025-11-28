package pl.kvgx12.wiertarbot.fb.config

import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.kvgx12.fbchat.session.Cookies
import java.io.File


@Serializable
data class JsonCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
)


fun loadCookies(file: String): Cookies {
    val json = Json { ignoreUnknownKeys = true }
    val content = File(file).readText()

    return json.decodeFromString<List<JsonCookie>>(content)
        .map {
            it.run {
                Cookie(
                    name = name,
                    value = value.decodeURLPart(),
                    domain = domain,
                    path = path,
                    secure = secure,
                    httpOnly = httpOnly,
                )
            }
        }
        .groupBy { Url("https://${it.domain!!}") }
}
