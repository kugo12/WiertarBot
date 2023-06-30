package pl.kvgx12.wiertarbot.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.MessageCountMilestone

@Repository
interface MessageCountMilestoneRepository : JpaRepository<MessageCountMilestone, Int> {
    fun findFirstByThreadId(threadId: String): MessageCountMilestone?
}
