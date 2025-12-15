package pl.kvgx12.wiertarbot.commands.ai

import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.command.dsl.text

val genAICommands = commands {
    if (env.getProperty("wiertarbot.genai.api-key") == null) {
        return@commands
    }

    bean<GenAI>()

    command("ai") {
        help(usage = "<prompt>", returns = "tekst")

        val client = dsl.ref<GenAI>()

        text {
            val text = it.text.split(' ', limit = 2)

            if (text.size == 2) {
                val prompt = text.last()

                if (prompt.isNotBlank()) {
                    return@text client.generate(text.last())
                }
            }

            help!!
        }
    }
}
