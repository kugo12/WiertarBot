package pl.kvgx12.wiertarbot.commands.ai

import kotlinx.coroutines.runBlocking
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import pl.kvgx12.wiertarbot.commands.clients.internal.CEXApi
import pl.kvgx12.wiertarbot.commands.clients.internal.CEXClient
import pl.kvgx12.wiertarbot.config.ContextHolder
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.utils.getLogger
import java.time.LocalDateTime
import java.time.ZoneId

class AITools(
    @Qualifier("searchChatClient")
    searchChatClientProvider: ObjectProvider<ChatClient>,
    private val contextHolder: ContextHolder,

    private val cexClient: CEXClient? = null,
) {
    private val log = getLogger()
    private val searchChatClient: ChatClient by lazy {
        checkNotNull(searchChatClientProvider.getIfAvailable())
    }

    @Tool
    fun getCurrentTime(): String = LocalDateTime.now(ZoneId.of("Europe/Warsaw")).toString()

    @Tool(description = "Search the web for information")
    fun webSearch(@ToolParam(description = "query") query: String): String = try {
        searchChatClient.prompt(query)
            .call()
            .content()
    } catch (e: Exception) {
        log.error("Error during web search for query: $query", e)
        null
    }.orEmpty()

    @Tool(description = "Execute code in a QuickJS environment")
    fun executeCode(@ToolParam(description = "single JS expression") code: String): String {
        if (cexClient == null) {
            log.warn("CEXClient is not configured, cannot execute code.")
            return "Code execution service is not available."
        }
        log.info("Executing code: $code")

        return try {
            val response = runBlocking {
                cexClient.executeCode(code)
            }

            when (response.errorCode) {
                CEXApi.ErrorCode.NONE -> response.result
                CEXApi.ErrorCode.TIMEOUT -> "Code execution timed out."
                CEXApi.ErrorCode.RUNTIME_ERROR -> "Runtime error during code execution."
            }
        } catch (e: Exception) {
            log.error("Error during code execution for code: $code", e)

            "An error occurred during code execution."
        }
    }

    @Tool
    fun reactToNewestMessage(
        @ToolParam(description = "single emoji. for telegram use basic emojis like 👍, ❤️, 😂, 😮, 😢, 😠")
        reactionEmoji: String,
        context: ToolContext
    ): String =
        try {
            val event = checkNotNull(context.context["messageEvent"] as? MessageEvent)

            runBlocking {
                contextHolder[event.connectorInfo.connectorType].reactToMessage(event, reactionEmoji)
            }

            "Reacted with '$reactionEmoji'."
        } catch (e: Exception) {
            log.error("Error reacting to message with reaction: $reactionEmoji", e)

            "Failed to react with '$reactionEmoji'."
        }
}
