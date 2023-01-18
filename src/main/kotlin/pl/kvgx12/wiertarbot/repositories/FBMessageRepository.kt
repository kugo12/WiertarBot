package pl.kvgx12.wiertarbot.repositories

import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.FBMessage
import reactor.core.publisher.Flux

@Repository
interface FBMessageRepository: R2dbcRepository<FBMessage, Int> {
    fun findAllByDeletedAtNullAndTimeBefore(time: Long): Flux<FBMessage>
    fun findAllByThreadIdAndDeletedAtNotNull(threadId: String, pageable: Pageable): Flux<FBMessage>

    @Modifying
    @Query("UPDATE FBMessage m SET m.deletedAt = :timestamp WHERE m.messageId = :messageId")
    suspend fun markDeleted(messageId: String, timestamp: Long)

    suspend fun deleteByDeletedAtNullAndTimeBefore(time: Long)
}