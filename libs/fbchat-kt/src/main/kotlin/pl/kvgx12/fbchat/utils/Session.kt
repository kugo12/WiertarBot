package pl.kvgx12.fbchat.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import kotlin.random.Random

internal typealias ServerJSDefine = Map<String, JsonElement>

internal object SessionUtils {
    data class Define(
        val dtsg: String,
        val revision: Int,
    )

    private data class Definition(
        val module: String,
        val method: JsonElement,
        val data: JsonElement,
        val arguments: JsonElement,
    ) {
        constructor(array: JsonArray) : this(
            module = array[0].tryAsString()
                ?: error("Module is not string ($array)"),
            method = array[1],
            data = array[2],
            arguments = array[3],
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun parseServerJsDefine(body: String): Define {
        val define = body
            .splitToSequence("{\"define\":")
            .drop(1)
            .ifEmpty { error("Could not find server js define") }
            .flatMap {
                it.byteInputStream().use { stream ->
                    Json.decodeToSequence<JsonArray>(stream, DecodeSequenceMode.WHITESPACE_SEPARATED).first()
                }
            }.associate { // TODO: filter?
                val definition = Definition(it.jsonArray)

                definition.module to definition.data
            }

        return Define(
            define.getFbDtsg() ?: error("Could not find dtsg in define"),
            define.getClientRevision(),
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
    }?.ifEmpty { null }

    fun getJsModsDefine(array: JsonArray): ServerJSDefine = buildMap {
        array.forEach {
            val definition = Definition(it.jsonArray)

            put(definition.module, definition.data)
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
                    put(
                        it.first().tryAsString()?.removeVersionFromModule().orEmpty(),
                        emptyJsonArray(),
                    )
                } else {
                    val definition = Definition(it)

                    put(
                        "${definition.module.removeVersionFromModule()}.${definition.method.tryAsString()}",
                        definition.arguments.tryAsArray() ?: emptyJsonArray(),
                    )
                }
            }
        }
    }

    private fun String.removeVersionFromModule(): String =
        split('@', limit = 2).first()

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
            } + ": code=$errorCode summary=$errorSummary description=$errorDescription",
        )
    }
}
