package pl.kvgx12.wiertarbot.commands.ai

import org.slf4j.LoggerFactory
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.manual
import pl.kvgx12.wiertarbot.command.dsl.special
import pl.kvgx12.wiertarbot.command.dsl.specialCommandName
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.proto.response

private val log = LoggerFactory.getLogger("pl.kvgx12.wiertarbot.commands.ai.Command")

fun ResponseData.toResponse(event: MessageEvent) = response {
    this@response.event = event
    this@response.replyToId = event.messageId
    this@response.text = this@toResponse.text
    this@response.mentions.addAll(
        this@toResponse.mentions.asIterable()
            .map {
                mention {
                    this.threadId = it.userId
                    this.offset = it.offset
                    this.length = it.length
                }
            }
    )
}

val aiCommand = command("ai") {
    help(usage = "(prompt)", returns = "tekst")

    val client = dsl.bean<AIService>()

    manual { event ->
        val text = event.text.split(' ', limit = 2)
        val last = text.getOrNull(1) ?: ""

        if (!event.hasReplyToId() && (text.size != 2 || last.isBlank())) {
            event.context.sendText(event, help!!)
            return@manual
        }

        val result = try {
            client.generate(event, last)
        } catch (e: Exception) {
            log.error("Error during generation: ", e)
            event.context.sendText(event, "Wystąpił błąd podczas generowania odpowiedzi AI")
            return@manual
        }

        val response = event.context.send(result.data.toResponse(event))
        if (response.messageId.isNotBlank()) {
            client.afterSuccessfulSend(result, response)
        }
    }
}

// FIXME permissions
val aiSpecialCommand = command(specialCommandName("ai")) {
    val client = dsl.bean<AIService>()
    val aiCommandPrefix = "${dsl.bean<WiertarbotProperties>().prefix}ai"

    special { event ->
        if (event.text.startsWith(aiCommandPrefix))
            return@special

        val conversationId = client.findConversationId(event)
            ?: return@special

        val result = try {
            client.generate(event, event.text, conversationId)
        } catch (e: Exception) {
            log.error("Error during generation: ", e)
            event.context.sendText(event, "Wystąpił błąd podczas generowania odpowiedzi AI")
            return@special
        }
        val response = event.context.send(result.data.toResponse(event))

        client.afterSuccessfulSend(result, response)
    }
}
