package pl.kvgx12.wiertarbot.commands.ai

import org.springframework.ai.chat.client.AdvisorParams
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.generic
import pl.kvgx12.wiertarbot.proto.mention
import pl.kvgx12.wiertarbot.utils.proto.Response

@Suppress("ConfigurationProperties")
@ConfigurationProperties("wiertarbot.genai")
data class GenAIProperties(
    val systemPrompt: String,
    val thinkingBudget: Int = 1024,
    val maxOutputTokens: Int = 2048,
    val temperature: Double = 1.0,
    val includeThoughts: Boolean = false,
    val googleSearchRetrieval: Boolean = false,
    val model: String = "gemini-flash-latest",
)

class GenAIRegistrar : BeanRegistrarDsl({
    if (env.getProperty("spring.ai.google.genai.api-key") != null) {
        registerBean {
            @EnableConfigurationProperties(GenAIProperties::class)
            object {}
        }

        registerBean<GenAI>()

        aiCommand()

        registerBean<ChatClient> {
            val props = bean<GenAIProperties>()
            val options = GoogleGenAiChatOptions.builder()
                // probably by default they're off, so just to be sure
                .safetySettings(
                    GoogleGenAiSafetySetting.HarmCategory.entries
                        .drop(1)
                        .map {
                            GoogleGenAiSafetySetting(
                                it,
                                GoogleGenAiSafetySetting.HarmBlockThreshold.OFF,
                                GoogleGenAiSafetySetting.HarmBlockMethod.HARM_BLOCK_METHOD_UNSPECIFIED,
                            )
                        },
                )
                .responseMimeType("application/json")
                .responseSchema(ResponseData.converter.schema)
                .googleSearchRetrieval(props.googleSearchRetrieval)
                .includeThoughts(props.includeThoughts)
                .thinkingBudget(props.thinkingBudget)
                .maxOutputTokens(props.maxOutputTokens)
                .temperature(props.temperature)
                .model(props.model)
                .build()

            bean<ChatClient.Builder>()
                .defaultSystem(props.systemPrompt)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultAdvisors(SimpleLoggerAdvisor())
                .defaultOptions(options)
                .build()
        }
    }
})

val aiCommand = command("ai") {
    help(usage = "<prompt>", returns = "tekst")

    val client = dsl.bean<GenAI>()

    generic { event ->
        val text = event.text.split(' ', limit = 2)

        if (text.size == 2) {
            val prompt = text.last()

            if (prompt.isNotBlank()) {
                val user = event.context.fetchThread(event.authorId)!!

                val response = client.generate(user, event, text.last())
                    ?: return@generic Response(event, text = "Niestety, nie udało się wygenerować odpowiedzi.")

                val mentions = response.mentions.map {
                    mention {
                        this.threadId = it.userId
                        this.offset = it.offset
                        this.length = it.length
                    }
                }

                return@generic Response(event, text = response.text, mentions = mentions, replyToId = event.externalId)
            }
        }

        Response(event, text = help!!)
    }
}
