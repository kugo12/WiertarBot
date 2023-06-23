package pl.kvgx12.fbchat.utils

import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DelegatedCookieStorage(
    private val delegate: CookiesStorage = AcceptAllCookiesStorage()
) : CookiesStorage {
    private val mutex = Mutex()
    private val urls = mutableSetOf<Url>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun get(requestUrl: Url): List<Cookie> =
        delegate.get(requestUrl)

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie): Unit =
        delegate.addCookie(requestUrl, cookie).also {
            scope.launch {
                newCookie(requestUrl)
            }
        }

    override fun close() {
        scope.cancel()
        delegate.close()
    }

    suspend fun getAllCookies() = mutex.withLock { urls.associateWith { get(it) } }

    private suspend fun newCookie(url: Url) {
        mutex.withLock {
            urls.add(url)
        }
    }
}
