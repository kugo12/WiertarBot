package pl.kvgx12.wiertarbot.config

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import org.springframework.scheduling.annotation.Scheduled
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.repositories.PostgresCacheRepository
import pl.kvgx12.wiertarbot.utils.getLogger
import java.time.Duration

data class PostgresCacheConfig(
    val name: String,
    val ttl: Duration? = null,
)

class CacheService(
    private val repository: PostgresCacheRepository,
    props: WiertarbotProperties,
) {
    private val log = getLogger()

    private val coroutineCaches = props.caches.associate { config ->
        config.name to PostgresCache(repository, config)
    }

    operator fun get(name: String): PostgresCache = checkNotNull(coroutineCaches[name])

    @Scheduled(cron = "0 0 * * * *")
    suspend fun clearTtlExpired() {
        val deleted = repository.clearTtlExpired()
        log.info("Cleared $deleted expired cache entries.")
    }
}

@OptIn(ExperimentalSerializationApi::class)
class PostgresCache(
    private val repository: PostgresCacheRepository,
    val config: PostgresCacheConfig,
) {
    private val cbor = Cbor { ignoreUnknownKeys = true }

    suspend fun <T : Any> get(key: Any, serializer: KSerializer<T>): T? {
        val row = repository.get(config.name, key.toString())

        return cbor.decodeFromByteArray(serializer, row ?: return null)
    }

    suspend fun <T> put(key: Any, serializer: KSerializer<T>, value: T): T? {
        if (value == null) {
            evict(key)
            return null
        }

        repository.put(
            config.name,
            key.toString(),
            cbor.encodeToByteArray(serializer, value),
            config.ttl,
        )

        return value
    }

    suspend inline fun <reified T : Any> get(key: Any): T? = get(key, serializer())

    suspend inline fun <reified T> put(key: Any, value: T) = put(key, serializer<T>(), value)

    suspend inline fun <reified T : Any> putIfAbsent(key: Any, func: () -> T): T {
        val existing = get<T>(key)
        if (existing != null) {
            return existing
        }

        val value = func()
        put(key, value)
        return value
    }


    suspend inline fun <reified T : Any> putIfAbsent(key: Any, serializer: KSerializer<T>, func: () -> T): T {
        val existing = get(key, serializer)
        if (existing != null) {
            return existing
        }

        val value = func()
        put(key, serializer, value)
        return value
    }

    suspend fun evict(key: Any) {
        repository.evict(config.name, key.toString())
    }

    suspend fun clear() {
        repository.clear(config.name)
    }
}
