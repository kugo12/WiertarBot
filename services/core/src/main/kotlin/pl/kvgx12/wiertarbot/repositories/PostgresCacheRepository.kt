package pl.kvgx12.wiertarbot.repositories

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.bind
import java.time.Duration
import java.time.Instant

class PostgresCacheRepository(private val databaseClient: DatabaseClient) {
    suspend fun get(name: String, key: String): ByteArray? {
        return databaseClient.sql("SELECT value FROM cache_entries WHERE name = :name AND key = :key AND (expires_at IS NULL OR expires_at > NOW())")
            .bind("name", name)
            .bind("key", key)
            .map { row, _ -> row.get("value", ByteArray::class.java)!! }
            .one()
            .awaitSingleOrNull()
    }

    suspend fun put(name: String, key: String, value: ByteArray, ttl: Duration?) {
        val expiresAt = ttl?.let { Instant.now().plus(it) }

        databaseClient.sql(
            """
            INSERT INTO cache_entries (name, key, value, expires_at) VALUES (:name, :key, :value, :expiresAt)
            ON CONFLICT (name, key) DO UPDATE SET value = :value, expires_at = :expiresAt
            """
        )
            .bind("name", name)
            .bind("key", key)
            .bind("value", value)
            .bind("expiresAt", expiresAt)
            .then()
            .awaitSingleOrNull()
    }

    suspend fun evict(name: String, key: String) {
        databaseClient.sql("DELETE FROM cache_entries WHERE name = :name AND key = :key")
            .bind("name", name)
            .bind("key", key)
            .then()
            .awaitSingleOrNull()
    }

    suspend fun clear(name: String) {
        databaseClient.sql("DELETE FROM cache_entries WHERE name = :name")
            .bind("name", name)
            .then()
            .awaitSingleOrNull()
    }

    suspend fun clearTtlExpired(): Long {
        return databaseClient.sql("DELETE FROM cache_entries WHERE expires_at IS NOT NULL AND expires_at <= NOW()")
            .fetch()
            .rowsUpdated()
            .awaitSingleOrNull() ?: 0
    }
}
