@file:OptIn(ExperimentalSerializationApi::class)

package pl.kvgx12.toon

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import pl.kvgx12.toon.Toon.Companion.DEFAULT_DELIMITER
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class Toon(
    val delimiter: Char = DEFAULT_DELIMITER,
    val keyFolding: KeyFolding = KeyFolding.Off,
    val flattenDepth: Int = Int.MAX_VALUE,
    val indent: Int = 2,
) {
    companion object {
        const val DEFAULT_DELIMITER = ','
    }

    enum class KeyFolding {
        Off, Safe
    }
}

class ToonEncoder(private val toon: Toon) {
    private val builder = StringBuilder()

    internal fun encode(value: JsonElement, nesting: Int = 0, arrayElement: Boolean = false) {
        val value = if (toon.keyFolding != Toon.KeyFolding.Off && nesting == 0) flatten(value) else value

        when (value) {
            is JsonPrimitive -> encodePrimitive(value)
            is JsonArray -> encodeArray(value, nesting)
            is JsonObject -> encodeObject(value, nesting, arrayElement)
        }
    }

    private fun encodeObject(value: JsonObject, nesting: Int, arrayElement: Boolean) {
        value.entries.forEachIndexed { index, (key, entryValue) ->
            if (index > 0 || (!arrayElement && nesting > 0)) builder.append('\n')
            if (nesting > 0 && (!arrayElement || index > 0)) builder.append(" ".repeat(nesting * toon.indent))

            encodeString(key, isKey = true)

            when (entryValue) {
                is JsonArray -> encodeArray(entryValue, nesting)
                is JsonPrimitive -> {
                    builder.append(": ")
                    encodePrimitive(entryValue)
                }

                else -> {
                    builder.append(':')
                    encode(entryValue, nesting + 1)
                }
            }
        }
    }

    private fun encodeArray(value: JsonArray, nesting: Int) {
        builder.append('[')
        builder.append(value.size)
        if (toon.delimiter != DEFAULT_DELIMITER) {
            builder.append(toon.delimiter)
        }
        builder.append(']')

        when {
            value.all { it is JsonPrimitive } -> {
                builder.append(':')
                encodePrimitiveArray(value)
            }

            isUniformObjectArray(value) -> encodeUniformObjectArray(value, nesting)
            else -> {
                builder.append(':')
                encodeGenericArray(value, nesting)
            }
        }
    }

    private fun encodePrimitive(value: JsonPrimitive) {
        if (value.isString) {
            encodeString(value.content, isKey = false)
            return
        }

        val booleanValue = value.booleanOrNull
        if (booleanValue != null) {
            builder.append(booleanValue.toString())
            return
        }

        builder.append(formatNumber(value.content))
    }

    private fun formatNumber(raw: String): String = try {
        val number = BigDecimal(raw)
        val formatter = DecimalFormat("0", DecimalFormatSymbols(Locale.US)).apply {
            isGroupingUsed = false
            maximumFractionDigits = 340
            minimumFractionDigits = 0
        }
        formatter.format(number)
    } catch (ex: NumberFormatException) {
        raw
    }

    private fun encodeString(value: String, isKey: Boolean) {
        val needsQuotes = value.isEmpty()
            || value.startsWith(' ') || value.endsWith(' ')
            || value.startsWith('-')
            || (value.startsWith('[') && value.endsWith(']'))
            || (value.startsWith('{') && value.endsWith('}'))
            || value in arrayOf("true", "false", "null")
            || value.toDoubleOrNull() != null
            || (isKey && !isUnquotedKey(value))
            || value.any { it in "\"\\\n\t\r:" || it == toon.delimiter || (isKey && it == ' ') }

        if (!needsQuotes) {
            builder.append(value)
            return
        }

        builder.append('"')
        for (char in value) {
            when (char) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\n' -> builder.append("\\n")
                '\t' -> builder.append("\\t")
                '\r' -> builder.append("\\r")
                else -> builder.append(char)
            }
        }
        builder.append('"')
    }

    private fun isIdentifierSegment(key: String): Boolean =
        key.isNotEmpty()
            && key.first().isValidIdentifierStart()
            && key.drop(1).all { it.isValidIdentifierChar() }

    private fun isUnquotedKey(key: String): Boolean =
        key.isNotEmpty()
            && key.first().isValidIdentifierStart()
            && key.drop(1).all { it.isValidIdentifierChar() || it == '.' }

    private fun Char.isValidIdentifierStart() = this in 'a'..'z' || this in 'A'..'Z' || this == '_'
    private fun Char.isValidIdentifierChar() = isValidIdentifierStart() || this in '0'..'9'

    fun flatten(value: JsonElement): JsonElement {
        if (value !is JsonObject) {
            return value
        }

        val originalKeys = value.keys

        return JsonObject(value.entries.associate { (key, element) ->
            if (!isIdentifierSegment(key)) {
                return@associate key to element
            }

            val (segments, leafValue) = collectSegments(key, element)
            val foldDepth = minOf(segments.size, toon.flattenDepth)
            if (foldDepth < 2 || (leafValue is JsonObject && leafValue.isNotEmpty())) {
                return@associate key to element
            }

            val foldedKey = segments.take(foldDepth).joinToString(".")
            if (foldedKey in originalKeys) {
                return@associate key to element
            }

            val foldedValue = segments.drop(foldDepth).foldRight(leafValue) { segment, acc ->
                buildJsonObject {
                    put(segment, acc)
                }
            }

            foldedKey to foldedValue
        })
    }

    private fun collectSegments(key: String, element: JsonElement): Pair<List<String>, JsonElement> {
        val segments = mutableListOf(key)
        var current = element

        while (current is JsonObject && current.size == 1) {
            val (childKey, childValue) = current.entries.first()
            if (!isIdentifierSegment(childKey)) break
            segments.add(childKey)
            current = childValue
        }

        return segments to current
    }

    private fun isUniformObjectArray(value: JsonArray): Boolean =
        value.all { it is JsonObject }
            && value.map { it.jsonObject.keys }.distinct().size == 1
            && value.all { it.jsonObject.values.all { v -> v is JsonPrimitive } }

    private fun encodePrimitiveArray(value: JsonArray) {
        if (value.isEmpty()) {
            return
        }

        builder.append(' ')
        value.forEachIndexed { index, element ->
            if (index > 0) {
                builder.append(toon.delimiter)
            }
            encodePrimitive(element as JsonPrimitive)
        }
    }

    private fun encodeUniformObjectArray(value: JsonArray, nesting: Int) {
        val keys = value.first().jsonObject.keys
        val indentStr = " ".repeat(toon.indent * (nesting + 1))

        builder.append('{')
        keys.forEachIndexed { index, key ->
            if (index > 0) {
                builder.append(toon.delimiter)
            }
            encodeString(key, isKey = true)
        }
        builder.append("}:")

        for (element in value) {
            builder.append('\n').append(indentStr)
            keys.forEachIndexed { index, key ->
                if (index > 0) {
                    builder.append(toon.delimiter)
                }
                encodePrimitive((element.jsonObject[key] ?: JsonNull).jsonPrimitive)
            }
        }
    }

    private fun encodeGenericArray(value: JsonArray, nesting: Int) {
        val indentStr = " ".repeat((nesting + 1) * toon.indent)

        for (element in value) {
            builder.append('\n').append(indentStr).append('-')
            if (element !is JsonObject || element.isNotEmpty()) {
                builder.append(' ')
            }

            val nextNesting = if (element is JsonArray) nesting + 1 else nesting + 2
            encode(element, nextNesting, arrayElement = true)
        }
    }

    override fun toString(): String = builder.toString()
}


fun <T> Toon.encodeToToonString(serializer: KSerializer<T>, value: T): String {
    val toon = ToonEncoder(this)
    toon.encode(Json.encodeToJsonElement(serializer, value))

    return toon.toString()
}

inline fun <reified T> Toon.encodeToToonString(value: T): String =
    encodeToToonString(serializer(), value)
