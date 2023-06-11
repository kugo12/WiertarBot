package pl.kvgx12.downloadapi.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class MutexHolder<Key> {
    private data class MutexRef(val mutex: Mutex, var count: Int)

    private val mutexes: MutableMap<Key, MutexRef> = mutableMapOf()
    private val mutex = Mutex()

    suspend fun get(key: Key): Mutex = mutex.withLock {
        val ref = mutexes.get(key)?.also {
            it.count++
        } ?: MutexRef(Mutex(), 1)

        ref.mutex
    }

    suspend fun remove(key: Key) {
        mutex.withLock {
            val ref = mutexes[key] ?: return@withLock

            if (--ref.count == 0) mutexes.remove(key)
        }
    }

    suspend inline fun <T> withLock(key: Key, func: () -> T): T {
        try {
            return get(key).withLock(action = func)
        } finally {
            remove(key)
        }
    }
}
