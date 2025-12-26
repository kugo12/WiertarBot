package pl.kvgx12.wiertarbot.commands.ai

import kotlinx.coroutines.flow.collect
import org.springframework.ai.chat.messages.Message
import org.springframework.scheduling.annotation.Scheduled
import pl.kvgx12.wiertarbot.entities.AIMessage.Companion.toEntity
import pl.kvgx12.wiertarbot.repositories.AIMessageRepository
import pl.kvgx12.wiertarbot.utils.getLogger
import java.time.LocalDateTime

open class AIMessageService(
    private val props: GenAIProperties,
    private val repository: AIMessageRepository,
) : ChatMemory {
    private val maxMessages = props.memoryWindow

    override suspend fun add(conversationId: String, message: Message) {
        check(conversationId.isNotBlank())

        repository.save(message.toEntity(conversationId))
    }

    override suspend fun add(conversationId: String, messages: List<Message>) {
        check(conversationId.isNotBlank())

        repository.saveAll(
            messages.map { it.toEntity(conversationId) }
        ).collect()
    }

    override suspend fun get(conversationId: String): List<Message> {
        check(conversationId.isNotBlank())

        return repository.findByConversationIdOrderByIdLimitBy(conversationId, maxMessages)
            .map { it.toMessage() }
    }

    override suspend fun findConversationIdByMessageId(messageId: String): String? {
        check(messageId.isNotBlank())

        return repository.findConversationIdByMessageId(messageId)
    }

    override suspend fun applyRetention(conversationId: String) {
        check(conversationId.isNotBlank())

        if (props.applyConversationRetention) {
            log.info("Applying retention for conversation $conversationId")

            val count = repository.deleteMessagesOlderThanNMessage(conversationId, maxMessages)
            if (count > 0) {
                log.info("Deleted $count messages from $conversationId")
            }
        }
    }

    @Scheduled(cron = $$"${wiertarbot.genai.retention-global-cron:0 0 3 * * *}")
    suspend fun applyGlobalRetention() {
        if (props.globalRetention != null) {
            log.info("Applying global retention")

            val count = repository.deleteByCreatedAtLessThanEqual(
                LocalDateTime.now().minus(props.globalRetention)
            )
            if (count > 0) {
                log.info("Deleted $count messages")
            }
        }
    }

    companion object {
        private val log = getLogger()
    }
}
