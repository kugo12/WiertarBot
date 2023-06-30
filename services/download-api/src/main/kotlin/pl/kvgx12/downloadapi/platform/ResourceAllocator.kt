package pl.kvgx12.downloadapi.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.kvgx12.downloadapi.utils.io
import java.io.File

interface Resource {
    suspend fun deallocate()
}

class FileResource(
    val file: File,
) : Resource {
    override suspend fun deallocate() {
        file.delete()
    }
}

class ResourceAllocator {
    private val resources: MutableList<Resource> = mutableListOf()
    private val mutex = Mutex()

    suspend fun cleanup() = coroutineScope {
        mutex.withLock {
            resources.toList().also {
                resources.clear()
            }
        }.map {
            async(Dispatchers.IO) { it.deallocate() }
        }.awaitAll()

        Unit
    }

    suspend fun allocateTempFile(prefix: String, suffix: String): File =
        io { File.createTempFile(prefix, suffix) }
            .also {
                mutex.withLock {
                    resources.add(FileResource(it))
                }
            }
}

suspend inline fun <T> withResourceAllocator(func: ResourceAllocator.() -> T): T {
    val allocator = ResourceAllocator()

    return try {
        func(allocator)
    } finally {
        allocator.cleanup()
    }
}
