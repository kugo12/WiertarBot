package pl.kvgx12.fbchat.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import kotlin.random.Random

internal typealias ServerJSDefine = Map<String, JsonElement>

internal object SessionUtils {
    data class Define(
        val dtsg: String,
        val revision: Int
    )

    private val log = LoggerFactory.getLogger(SessionUtils::class.java)

    @OptIn(ExperimentalSerializationApi::class)
    fun parseServerJsDefine(body: String): Define {
        val define = body
            .splitToSequence("{\"define\":")
            .drop(1)
            .ifEmpty { error("Could not find server js define") }
            .flatMap {
                it.byteInputStream().use {
                    Json.decodeToSequence<JsonArray>(it, DecodeSequenceMode.WHITESPACE_SEPARATED).first()
                }
            }.associate { // TODO: filter?
                val (module, _, data, _) = it.tryAsArray()
                    ?: error("$it is not an array")

                val key = module.tryAsString() ?: error("$module is not a string ($it)")

                key to data
            }

        return Define(
            define.getFbDtsg() ?: error("Could not find dtsg in define"),
            define.getClientRevision()
        )
    }

    private fun ServerJSDefine.getClientRevision() =
        get("SiteData")?.tryGet("client_revision")?.tryAsInt()
            ?: error("Couldn't get client revision")

    fun ServerJSDefine.getFbDtsg() = when {
        "DTSGInitData" in this ->
            this["DTSGInitData"]?.tryGet("token")?.tryAsString()

        "DTSGInitialData" in this ->
            this["DTSGInitialData"]?.tryGet("token")?.tryAsString()

        "MRequestConfig" in this ->
            this["MRequestConfig"]?.tryGet("dtsg")?.tryGet("token")?.tryAsString()

        else -> null
    }

    fun getJsModsDefine(array: JsonArray): ServerJSDefine = buildMap {
        array.forEach {
            val (module, _, data, _) = it.jsonArray

            put(
                module.jsonPrimitive.content,
                data
            )
        }
    }

    fun generateOfflineThreadingId(): String {
        val now = System.currentTimeMillis()
        val value = Random.nextInt()

        val id = (now shl 22) or (value and 0x3FFFFF).toLong()

        return id.toString()
    }

    fun generateMessageId(now: Long, clientId: String) =
        "<$now:${Random.nextInt()}-$clientId@mail.projektitan.com>"

    fun JsonArray.getJsmodsRequire() = buildMap<String, JsonArray> {
        this@getJsmodsRequire.forEach { element ->
            element.tryAsArray()?.let {
                if (it.size == 1) {
                    put(it.first().removeVersionFromModule(), emptyJsonArray())
                } else {
                    val (module, method, _, arguments) = it

                    put(
                        "${module.removeVersionFromModule()}.${method.tryAsString()}",
                        arguments.tryAsArray() ?: emptyJsonArray()
                    )
                }
            }
        }
    }

    private fun JsonElement.removeVersionFromModule(): String =
        tryAsString()
            ?.split('@', limit = 2)
            ?.first()
            ?: ""

    fun JsonObject.handlePayloadError(ignoreJsmodRedirect: Boolean = false): JsonObject {
        if (!ignoreJsmodRedirect) {
            get("jsmods")
                ?.tryGet("require")
                ?.tryAsArray()
                ?.getJsmodsRequire()
                ?.get("ServerRedirect.redirectPageTo")
                ?.firstOrNull()
                ?.let {
                    // TODO
                    error("Got server redirect to $it")
                }
        }

        val errorCode = get("error") ?: return this
        val errorSummary = get("errorSummary")
        val errorDescription = get("errorDescription")

        error(
            when (errorCode.tryAsInt()) {
                1357001 -> "Not logged in"
                1357004 -> "Please refresh"
                1357031, 1545010, 1545003 -> "Invalid parameters"
                else -> "Unknown error"
            } + ": code=$errorCode summary=$errorSummary description=$errorDescription"
        )
    }
}
