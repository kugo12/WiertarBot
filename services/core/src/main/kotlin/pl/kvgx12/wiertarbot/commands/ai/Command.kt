package pl.kvgx12.wiertarbot.commands.ai

import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.genericWithCallback
import pl.kvgx12.wiertarbot.command.dsl.special
import pl.kvgx12.wiertarbot.command.dsl.specialCommandName
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.proto.response
import pl.kvgx12.wiertarbot.utils.proto.Response

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
    help(usage = "<prompt>", returns = "tekst")

    val client = dsl.bean<AIService>()

    genericWithCallback { event ->
        val text = event.text.split(' ', limit = 2)
        val last = text.last()

        if (text.size == 2 && last.isNotBlank()) {
            val result = client.generate(event, text.last())

            return@genericWithCallback result.data.toResponse(event) to {
                if (it.messageId.isNotBlank()) {
                    client.afterSuccessfulSend(result, it)
                }
            }
        }

        Response(event, text = help!!) to null
    }
}

val aiSpecialCommand = command(specialCommandName("ai")) {
    val client = dsl.bean<AIService>()
    val aiCommandPrefix = "${dsl.bean<WiertarbotProperties>().prefix}ai"

    special { event ->
        if (event.text.startsWith(aiCommandPrefix))
            return@special

        val conversationId = client.findConversationId(event)
            ?: return@special

        val result = client.generate(event, event.text, conversationId)
        val response = event.context.send(result.data.toResponse(event))

        client.afterSuccessfulSend(result, response)
    }
}
