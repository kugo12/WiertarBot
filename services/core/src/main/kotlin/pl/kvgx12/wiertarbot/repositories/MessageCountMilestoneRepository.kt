package pl.kvgx12.wiertarbot.repositories

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.MessageCountMilestone

@Repository
interface MessageCountMilestoneRepository : CoroutineCrudRepository<MessageCountMilestone, Int> {
    suspend fun findFirstByThreadId(threadId: String): MessageCountMilestone?
}
