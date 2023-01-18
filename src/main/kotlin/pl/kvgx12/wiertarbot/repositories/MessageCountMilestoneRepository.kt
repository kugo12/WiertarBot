package pl.kvgx12.wiertarbot.repositories

import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.MessageCountMilestone

@Repository
interface MessageCountMilestoneRepository: R2dbcRepository<MessageCountMilestone, Int> {
    suspend fun findFirstByThreadId(threadId: String): MessageCountMilestone?
}