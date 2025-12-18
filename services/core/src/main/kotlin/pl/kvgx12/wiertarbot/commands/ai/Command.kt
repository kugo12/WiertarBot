package pl.kvgx12.wiertarbot.commands.ai

import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.genericWithCallback
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.utils.proto.Response

val aiCommand = command("ai") {
    help(usage = "<prompt>", returns = "tekst")

    val client = dsl.bean<AIService>()

    genericWithCallback { event ->
        val text = event.text.split(' ', limit = 2)
        val last = text.last()

        if (text.size == 2 && last.isNotBlank()) {
            val result = client.generate(event, text.last())
                ?: return@genericWithCallback Response(event, text = "Niestety, nie udało się wygenerować odpowiedzi.") to null

            val mentions = result.data.mentions.map {
                mention {
                    this.threadId = it.userId
                    this.offset = it.offset
                    this.length = it.length
                }
            }

            return@genericWithCallback Response(event, text = result.data.text, mentions = mentions, replyToId = event.messageId) to {
                if (it.messageId.isNotBlank()) {
                    client.afterSuccessfulSend(result, it)
                }
            }
        }

        Response(event, text = help!!) to null
    }
}
