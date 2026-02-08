package pl.kvgx12.wiertarbot.commands.ai

import kotlinx.coroutines.runBlocking
import org.springframework.ai.chat.model.ToolContext
import org.springframework.ai.tool.annotation.Tool
import pl.kvgx12.wiertarbot.config.ContextHolder
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.services.CachedContextService
import pl.kvgx12.wiertarbot.utils.proto.Response

class KillUserTool(
    private val contextHolder: ContextHolder,
    private val cachedContextService: CachedContextService,
) : AITool {
    @Tool(description = "Kill a user in current thread by their ID, warning: will cause serious harm and health issues to the user, use with caution")
    fun killUser(userId: String, reason: String, context: ToolContext): String {
        val event = checkNotNull(context.context["messageEvent"] as? MessageEvent)
        val user = runBlocking {
            cachedContextService.getThread(event.connectorInfo.connectorType, event.threadId)
                ?.participantsList
                ?.first { it.id == userId }
        } ?: return "Thread or user not found."

        runBlocking {
            val base = "Zabiłem "
            val mention = '@' + user.customizedName.ifEmpty { user.name }

            contextHolder[event.connectorInfo.connectorType].send(
                Response(
                    event,
                    text = "$base$mention z powodu: $reason",
                    mentions = listOf(
                        mention {
                            threadId = userId
                            offset = base.length
                            length = mention.length
                        }
                    )
                )
            )
        }
        return "User with ID $userId has been killed."
    }
}
