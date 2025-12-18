package pl.kvgx12.wiertarbot.commands.ai

import org.springframework.ai.chat.client.AdvisorParams
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import java.time.Duration

@Suppress("ConfigurationProperties")
@ConfigurationProperties("wiertarbot.genai")
data class GenAIProperties(
    val systemPrompt: String,
    val thinkingBudget: Int = 1024,
    val maxOutputTokens: Int = 2048,
    val temperature: Double = 1.0,
    val applyConversationRetention: Boolean = false,
    val globalRetention: String? = null,
    val includeThoughts: Boolean = false,
    val googleSearchRetrieval: Boolean = false,
    val model: String = "gemini-flash-latest",
    val memoryWindow: Int = 20,
) {
    val globalRetentionDuration = globalRetention?.let { Duration.parse(it) }

    init {
        require(systemPrompt.isNotBlank()) { "System prompt must not be blank" }
        require(thinkingBudget > 0) { "Thinking budget must be greater than 0" }
        require(maxOutputTokens > 0) { "Max output tokens must be greater than 0" }
        require(temperature in 0.0..2.0) { "Temperature must be between 0.0 and 2.0" }
        require(memoryWindow > 0) { "Memory window must be greater than 0" }
    }
}

class GenAIRegistrar : BeanRegistrarDsl({
    if (env.getProperty("spring.ai.google.genai.api-key") != null) {
        registerBean {
            @EnableConfigurationProperties(GenAIProperties::class)
            object {}
        }

        registerBean<AIService>()
        registerBean<AIMessageService>()

        aiCommand()
        aiSpecialCommand()

        registerBean<ChatClient> {
            val props = bean<GenAIProperties>()
            val options = GoogleGenAiChatOptions.builder()
                // probably by default they're off, so just to be sure
                .safetySettings(
                    GoogleGenAiSafetySetting.HarmCategory.entries
                        .drop(1)  // drop unspecified
                        .map {
                            GoogleGenAiSafetySetting(
                                it,
                                GoogleGenAiSafetySetting.HarmBlockThreshold.OFF,
                                GoogleGenAiSafetySetting.HarmBlockMethod.HARM_BLOCK_METHOD_UNSPECIFIED,
                            )
                        },
                )
                .responseMimeType("text/plain")
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
