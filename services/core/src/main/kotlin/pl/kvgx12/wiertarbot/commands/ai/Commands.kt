package pl.kvgx12.wiertarbot.commands.ai

import org.springframework.beans.factory.BeanRegistrarDsl
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.text

class GenAICommandsRegistrar : BeanRegistrarDsl({
    if (env.getProperty("wiertarbot.genai.api-key") != null) {
        registerBean<GenAI>()

        command("ai") {
            help(usage = "<prompt>", returns = "tekst")

            val client = dsl.bean<GenAI>()

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
})
