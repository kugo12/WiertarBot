package pl.kvgx12.wiertarbot.repositories

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import pl.kvgx12.wiertarbot.entities.AIMessage
import java.time.LocalDateTime

@Repository
interface AIMessageRepository : CoroutineCrudRepository<AIMessage, Long> {
    suspend fun deleteByConversationId(conversationId: String)

    @Query(
        """
        SELECT sub.*
        FROM (SELECT * FROM ai_message WHERE conversation_id = :conversationId ORDER BY id DESC LIMIT :limit) AS sub
        ORDER BY sub.id
        """
    )
    suspend fun findByConversationIdOrderByIdLimitBy(conversationId: String, limit: Int): List<AIMessage>

    @Query("SELECT conversation_id FROM ai_message WHERE message_id = :messageId LIMIT 1")
    suspend fun findConversationIdByMessageId(messageId: String): String?

    @Modifying
    @Query("UPDATE ai_message SET message_id = :messageId WHERE conversation_id = :conversationId AND message_id = :tempMessageId")
    suspend fun backfillMessageIdByTemp(conversationId: String, tempMessageId: String, messageId: String)

    @Modifying
    @Query(
        """
        DELETE FROM ai_message
        WHERE conversation_id = :conversationId
        AND id <= (
            SELECT id FROM ai_message WHERE conversation_id = :conversationId
            ORDER BY id DESC LIMIT 1 OFFSET :n
            )"""
    )
    suspend fun deleteMessagesOlderThanNMessage(conversationId: String, n: Int): Int

    @Modifying
    suspend fun deleteByCreatedAtLessThanEqual(createdAtLT: LocalDateTime): Int
}
