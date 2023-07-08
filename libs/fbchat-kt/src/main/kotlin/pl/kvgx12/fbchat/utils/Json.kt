package pl.kvgx12.fbchat.utils

import kotlinx.serialization.json.*

internal fun JsonElement?.tryGet(key: String) = (this as? JsonObject)?.get(key)
internal fun JsonElement?.tryAsString() = (this as? JsonPrimitive)?.contentOrNull
internal fun JsonElement?.tryAsInt() = (this as? JsonPrimitive)?.intOrNull
internal fun JsonElement?.tryAsArray() = this as? JsonArray

internal fun emptyJsonArray() = JsonArray(emptyList())
