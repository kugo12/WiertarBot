package pl.kvgx12.wiertarbot.commands.ai

import org.springframework.ai.chat.messages.Message

interface ChatMemory {
    suspend fun add(conversationId: String, message: Message)

    suspend fun add(conversationId: String, messages: List<Message>)

    suspend fun get(conversationId: String): List<Message>

    suspend fun findConversationIdByMessageId(messageId: String): String?
    suspend fun applyRetention(conversationId: String)
}
