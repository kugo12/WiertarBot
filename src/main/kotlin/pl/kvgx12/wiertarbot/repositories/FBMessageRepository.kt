package pl.kvgx12.wiertarbot.repositories

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.FBMessage

@Repository
interface FBMessageRepository: JpaRepository<FBMessage, Int> {
    fun findAllByDeletedAtNullAndTimeBefore(time: Long): List<FBMessage>
    fun findAllByThreadIdAndDeletedAtNotNull(threadId: String, pageable: Pageable): List<FBMessage>

    @Modifying
    @Query("UPDATE fbmessage m SET m.deletedAt = :timestamp WHERE m.messageId = :messageId")
    fun markDeleted(messageId: String, timestamp: Long)

    fun deleteByDeletedAtNullAndTimeBefore(time: Long)
}