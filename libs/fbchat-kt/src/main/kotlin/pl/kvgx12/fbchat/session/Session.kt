package pl.kvgx12.fbchat.session

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.HttpTimeout.Plugin.INFINITE_TIMEOUT_MS
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.slf4j.LoggerFactory
import pl.kvgx12.fbchat.utils.*
import pl.kvgx12.fbchat.utils.SessionUtils.getFbDtsg
import pl.kvgx12.fbchat.utils.SessionUtils.handlePayloadError
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class Session internal constructor(
    val userId: String,
    var fbDtsg: String,
    val revision: Int,
    internal val client: HttpClient,
    private val cookieStorage: DelegatedCookieStorage,
) {
    internal var sequenceId: String? = null
    internal val clientId = UUID.randomUUID().toString()
    private val counter = AtomicInteger(0)

    suspend fun getAllCookies() = cookieStorage.getAllCookies()

    suspend fun logout() {
        val response = client.post(Messenger.Logout()) {
            bodyForm {
                getParams().forEach {
                    append(it.key, it.value)
                }
            }
        }.handleStatus()

        when (val location = response.locationHeader) {
            null -> error("Could not log out, was not redirected")
            client.href(Messenger.Login()) -> {}
            else -> error("Could not log out, was redirected to $location")
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val response = client.get(Messenger.Login()).handleStatus()

        return when (response.locationHeader) {
            client.href(Messenger()),
            client.href(Messenger.Checkpoint.Block()),
            -> true

            else -> false
        }
    }

    suspend fun get(url: Url) = client.get(url).handleStatus()

    internal suspend fun postBasic(
        url: String,
        data: Map<String, String>,
        files: List<FormPart<*>>? = null,
    ): String {
        val formData = (data + getParams())

        return client.post(url) {
            if (files != null) {
                bodyMultipartForm {
                    formData.forEach { append(it.key, it.value) }
                    files.forEach { append(it) }
                }
                timeout { requestTimeoutMillis = INFINITE_TIMEOUT_MS }
            } else {
                bodyForm {
                    formData.forEach { append(it.key, it.value) }
                }
            }
        }.handleStatus()
            .bodyAsText()
            .ifEmpty { error("Received empty body") }
            .let { stripJsonCruft(it) }
    }

    internal suspend inline fun <reified T : Any> post(
        url: T,
        data: Map<String, String>,
        files: List<FormPart<Any>>? = null,
    ) = postBasic(client.href(url), data, files)

    internal suspend fun payloadPost(
        url: String,
        data: Map<String, String>,
        files: List<FormPart<*>>? = null,
    ): JsonElement {
        val response = json.decodeFromString<JsonObject>(postBasic(url, data, files))

        (response["jsmods"] as? JsonArray)?.let {
            SessionUtils.getJsModsDefine(it)
                .getFbDtsg()
                ?.let { dtsg -> fbDtsg = dtsg }
        }

        return response["payload"] ?: error("Missing payload $response")
    }

    internal suspend fun doSendRequest(
        input: Map<String, String>,
    ): Pair<String, String?> {
        val now = System.currentTimeMillis()
        val offlineThreadingId = SessionUtils.generateOfflineThreadingId()

        val data = input + mapOf(
            "client" to "mercury",
            "author" to "fbid:$userId",
            "timestamp" to now.toString(),
            "source" to "source:chat:web",
            "offline_threading_id" to offlineThreadingId,
            "message_id" to offlineThreadingId,
            "threading_id" to SessionUtils.generateMessageId(now, clientId),
            "ephemeral_ttl_mode" to "0",
        )

        val response = post(Messenger.Messaging.Send(), data)
            .let { json.decodeFromString<JsonObject>(it) }
            .handlePayloadError()

        val ids = response["payload"].tryGet("actions").tryAsArray()
            ?.let { array ->
                array.asSequence()
                    .filterIsInstance<JsonObject>()
                    .filter { "message_id" in it }
                    .map { it["message_id"].tryAsString()!! to it["thread_fbid"].tryAsString() }
                    .toList()
                    .ifEmpty { null }
            } ?: error("No message ids found in $response")

        if (ids.size != 1) {
            log.warn("Got multiple message ids back: $ids")
        }

        return ids.first()
    }

    private fun getParams() = mapOf(
        "__a" to "1",
        "__req" to counter.incrementAndGet().toString(36),
        "__rev" to revision.toString(),
        "fb_dtsg" to fbDtsg,
        "server_timestamps" to "true",
        "fb_api_friendly_name" to "RelayModern",
    )

    override fun toString(): String = "Session(user_id=$userId)"

    companion object {
        internal val log = LoggerFactory.getLogger(Session::class.java)
        internal val baseUrl = Url("https://www.messenger.com/")

        internal val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        internal fun HttpResponse.handleStatus(): HttpResponse {
            if (status.value in 200 until 400) {
                return this
            }
            TODO()
        }
    }
}
