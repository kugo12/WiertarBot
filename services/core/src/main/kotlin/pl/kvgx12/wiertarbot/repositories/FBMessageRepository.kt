package pl.kvgx12.wiertarbot.repositories

import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.FBMessage

@Repository
interface FBMessageRepository : CoroutineCrudRepository<FBMessage, Int> {
    fun findAllByDeletedAtNullAndTimeBefore(time: Long): Flow<FBMessage>
    fun findAllByThreadIdAndDeletedAtNotNull(threadId: String, pageable: Pageable): Flow<FBMessage>

    @Modifying
    @Query("UPDATE fbmessage m SET m.deletedAt = :timestamp WHERE m.messageId = :messageId")
    suspend fun markDeleted(messageId: String, timestamp: Long)

    suspend fun deleteByDeletedAtNullAndTimeBefore(time: Long)
}
