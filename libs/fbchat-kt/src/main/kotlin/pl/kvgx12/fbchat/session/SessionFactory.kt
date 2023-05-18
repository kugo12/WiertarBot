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
import io.ktor.websocket.*
import it.skrape.selects.html5.form
import pl.kvgx12.fbchat.session.Session.Companion.baseUrl
import pl.kvgx12.fbchat.session.Session.Companion.handleStatus
import pl.kvgx12.fbchat.utils.*


suspend fun Session(
    email: String,
    password: String,
    secondFactorAuthHandler: suspend () -> Int = default2FAHandler
): Session {
    val (cookies, client) = createClient()

    val loginPage = client.get(Messenger.Login())
        .handleStatus()
        .bodyAsText()

    val formData = buildMap {
        loginPage.html {
            findAll("form input") {
                associateTo(this@buildMap) {
                    it.attribute("name") to it.attribute("value")
                }
            }
        }

        put("timezone", "-60")
        put("email", email)
        put("pass", password)
        put("login", "1")
        put("persistent", "1")  // long cookie expiry
        remove("default_persistent")
    }
    val datr = findDatr(loginPage) ?: error("Could not find datr")

    cookies.addCookie(baseUrl, Cookie("datr", datr))
    cookies.addCookie(baseUrl, Cookie("locale", "pl_PL"))

    val response = client.post(Messenger.Login.Password()) {
        bodyForm {
            formData.forEach {
                append(it.key, it.value)
            }
        }
    }.handleStatus()

    val url = response.locationHeader
        ?: error("Invalid credentials: ${getLoginErrorData(response.bodyAsText())}")

    if ("checkpoint" in url)
        TODO("Checkpoint login redirection not implemented")

    if (url != baseUrl.toString())
        error("Invalid login redirect: $url")

    return Session(client, cookies)
}

suspend fun Session(cookies: Map<Url, List<Cookie>>): Session {
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
    cookieStorage: DelegatedCookieStorage
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
        cookieStorage
    )
}

internal fun createClient(): Pair<DelegatedCookieStorage, HttpClient> {
    val cookies = DelegatedCookieStorage()

    return cookies to HttpClient(CIO) {
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

        followRedirects = false

        CurlUserAgent()

        defaultRequest {
            accept(ContentType.Any)
            header("sec-fetch-site", "same-origin")
            header(HttpHeaders.Referrer, baseUrl)
            url.takeFrom(baseUrl)
        }
    }
}

private fun findDatr(body: String): String? {
    val definitionIndex = body.indexOfOrNull(DATR_KEY)
        ?: return null
    val start = body.indexOfOrNull('"', definitionIndex + DATR_KEY.length + 1)
        ?: return null
    val end = body.indexOfOrNull('"', start + 1)
        ?: return null

    return body.substring(start + 1, end)
}

private val default2FAHandler: suspend () -> Int = { error("2FA handler was not passed in") }

private const val DATR_KEY = "\"_js_datr\""

private fun getLoginErrorData(body: String) = body.html {
    relaxed = true

    form {
        withId = "login_form"

        findFirst {
            children.getOrNull(2)?.text ?: ""
        }
    }
}
