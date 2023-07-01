package pl.kvgx12.downloadapi.utils

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.function.Predicate

val String.isUrl get() = runCatching { Url(this) }.isSuccess

inline val Url.lastPathSegment get() = pathSegments.last { it.isNotEmpty() }

fun Url.getSegmentAfter(string: String): String? {
    val index = pathSegments.indexOf(string)

    return if (index != -1) {
        pathSegments.getOrNull(index + 1)
    } else {
        null
    }
}

suspend inline fun <T> io(noinline func: suspend CoroutineScope.() -> T) = withContext(Dispatchers.IO, func)

@Suppress("FunctionNaming")
inline fun Url(url: String, func: URLBuilder.() -> Unit) = URLBuilder(url).apply(func).build()

@Suppress("FunctionNaming")
inline fun Url(url: Url, func: URLBuilder.() -> Unit) = URLBuilder(url).apply(func).build()

@Suppress("FunctionNaming")
inline fun Url(func: URLBuilder.() -> Unit) = URLBuilder().apply(func).build()

class HostPredicate(
    private val hosts: List<Host>,
) : Predicate<String> {
    constructor(vararg hosts: String) : this(hosts.map { Host(it.split('.')) })

    override fun test(t: String): Boolean {
        val host = t.split('.')

        return hosts.any { it.host == host.takeLast(it.host.size) }
    }

    @JvmInline
    value class Host(val host: List<String>)
}

fun Url.base64() = toString().encodeBase64()
