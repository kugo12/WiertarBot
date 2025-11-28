package pl.kvgx12.fbchat.session

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import io.ktor.websocket.*
import pl.kvgx12.fbchat.session.Session.Companion.baseUrl
import pl.kvgx12.fbchat.session.Session.Companion.handleStatus
import pl.kvgx12.fbchat.utils.DelegatedCookieStorage
import pl.kvgx12.fbchat.utils.SessionUtils

typealias Cookies = Map<Url, List<Cookie>>

suspend fun Session(cookies: Cookies): Session {
    val (storage, client) = createClient()

    cookies.forEach { (url, c) ->
        c.forEach {
            storage.addCookie(url, it)
        }
    }

    return Session(client, storage)
}

private suspend fun Session(
    client: HttpClient,
    cookieStorage: DelegatedCookieStorage,
): Session {
    val userId = client.cookies(baseUrl)
        .find { it.name == "c_user" }?.value
        ?: error("Could not find c_user cookie ")

    val body = client
        .config { followRedirects = true }
        .get(Messenger())
        .handleStatus()
        .bodyAsText()
        .ifEmpty { error("Unexpected empty body") }

    val define = SessionUtils.parseServerJsDefine(body)

    return Session(
        userId,
        define.dtsg,
        define.revision,
        client,
        cookieStorage,
    )
}

internal fun createClient(): Pair<DelegatedCookieStorage, HttpClient> {
    val cookies = DelegatedCookieStorage()
    val client = HttpClient(CIO) {
        followRedirects = false

        install(ContentNegotiation) {
            json(Session.json)
        }

        install(HttpTimeout)
        install(WebSockets) {
            extensions {
                install(WebSocketDeflateExtension)
            }
        }
        install(HttpCookies) {
            storage = cookies
        }
        install(Resources)
        install(Logging) {
            level = LogLevel.INFO
        }

        install(UserAgent) {
            agent = Session.userAgent
        }

        defaultRequest {
            accept(ContentType.Any)
            headers.appendIfNameAbsent("sec-fetch-site", "same-origin")
            headers.appendIfNameAbsent(HttpHeaders.Referrer, baseUrl.toString())
            url.takeFrom(baseUrl)
        }
    }

    return cookies to client
}
