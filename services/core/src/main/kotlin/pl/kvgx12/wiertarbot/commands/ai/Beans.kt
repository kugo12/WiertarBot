package pl.kvgx12.wiertarbot.commands.ai

import org.springframework.ai.chat.client.AdvisorParams
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
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
    val model: String = "gemini-flash-latest",
    val memoryWindow: Int = 20,
    val maxToolCalls: Int = 5,
    val maxSerializationRetries: Int = 1,
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

        registerBean<AITools>()
        registerBean<ToolCallbackProvider> {
            MethodToolCallbackProvider.builder()
                .toolObjects(bean<AITools>())
                .build()
        }

        aiCommand()
        aiSpecialCommand()

        registerBean<ChatOptions>("chatOptions", primary = true) {
            val props = bean<GenAIProperties>()

            GoogleGenAiChatOptions.builder()
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
                .googleSearchRetrieval(false)
                .includeThoughts(false)
                .internalToolExecutionEnabled(false)
                .thinkingBudget(props.thinkingBudget)
                .maxOutputTokens(props.maxOutputTokens)
                .temperature(props.temperature)
                .model(props.model)
                .build()
        }

        registerBean<ChatClient>("chatClient", primary = true) {
            val props = bean<GenAIProperties>()
            val options = bean<ChatOptions>("chatOptions")

            bean<ChatClient.Builder>()
                .defaultSystem("${props.systemPrompt}\n${ResponseData.converter.instruction}")
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultAdvisors(SimpleLoggerAdvisor())
                .defaultOptions(options)
                .defaultTools(bean<AITools>())
                .build()
        }

        registerBean<ChatOptions>("searchChatOptions") {
            val props = bean<GenAIProperties>()

            GoogleGenAiChatOptions.builder()
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
                .googleSearchRetrieval(true)
                .includeThoughts(false)
                .thinkingBudget(512)
                .maxOutputTokens(props.maxOutputTokens / 2)
                .temperature(1.0)
                .model("gemini-flash-latest")
                .build()
        }

        registerBean<ChatClient>("searchChatClient") {
            val options = bean<ChatOptions>("searchChatOptions")

            bean<ChatClient.Builder>()
                .defaultOptions(options)
                .defaultSystem("You are a helpful assistant that searches the web for information.\nBe as short as possible, include only relevant information.")
                .build()
        }
    }
})
