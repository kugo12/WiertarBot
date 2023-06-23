package pl.kvgx12.downloadapi.utils

import io.ktor.http.*
import kotlinx.serialization.json.*

// FIXME: ?
class FindUrlWithKeyTraversal(
    private val key: String
) {
    fun traverse(obj: JsonElement, key: String? = null): Sequence<Url> = sequence {
        when (obj) {
            is JsonArray -> obj.forEach {
                yieldAll(traverse(it, key))
            }

            is JsonObject -> obj.forEach { (k, v) ->
                yieldAll(traverse(v, k))
            }

            is JsonPrimitive ->
                if (
                    obj.isString
                    && obj.content.startsWith("https")
                    && key == this@FindUrlWithKeyTraversal.key
                ) {
                    yield(Url(obj.content))
                }

            JsonNull -> {}
        }
    }
}
