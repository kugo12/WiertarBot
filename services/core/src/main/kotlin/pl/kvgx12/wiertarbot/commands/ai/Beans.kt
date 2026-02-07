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
import org.springframework.core.env.getProperty
import java.time.Duration

@Suppress("ConfigurationProperties")
@ConfigurationProperties("wiertarbot.genai")
data class GenAIProperties(
    val memoryWindow: Int = 20,
    val maxToolCalls: Int = 5,
    val maxSerializationRetries: Int = 1,
    val applyConversationRetention: Boolean = false,
    val globalRetention: Duration? = null,
    val killUserEnabled: Boolean = false,

    val model: ModelConfig,
    val searchTool: ModelConfig,
) {
    data class ModelConfig(
        val systemPrompt: String,
        val thinkingBudget: Int,
        val maxOutputTokens: Int,
        val temperature: Double = 1.0,
        val model: String = "gemini-flash-latest",
    ) {
        init {
            require(thinkingBudget > 0) { "Thinking budget must be greater than 0" }
            require(maxOutputTokens > 0) { "Max output tokens must be greater than 0" }
            require(temperature in 0.0..2.0) { "Temperature must be between 0.0 and 2.0" }
            require(systemPrompt.isNotBlank()) { "System prompt must not be blank" }
        }
    }

    init {
        require(memoryWindow > 0) { "Memory window must be greater than 0" }
    }
}

@JvmInline
value class AIToolArray(val tools: Array<AITool>)

class GenAIRegistrar : BeanRegistrarDsl({
    if (env.getProperty("spring.ai.google.genai.api-key") != null) {
        registerBean {
            @EnableConfigurationProperties(GenAIProperties::class)
            object {}
        }

        registerBean<AIService>()
        registerBean<AIMessageService>()

        registerBean<AITools>()
        if (env.getProperty<Boolean>("wiertarbot.genai.kill-user-enabled", false)) {
            registerBean<KillUserTool>()
        }
        registerBean<AIToolArray> {
            AIToolArray(
                beanProvider<AITool>()
                    .toList()
                    .toTypedArray()
            )
        }
        registerBean<ToolCallbackProvider> {
            MethodToolCallbackProvider.builder()
                .toolObjects(*bean<AIToolArray>().tools)
                .build()
        }

        aiCommand()
        aiSpecialCommand()

        registerBean<ChatOptions>("chatOptions", primary = true) {
            val m = bean<GenAIProperties>().model

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
                .thinkingBudget(m.thinkingBudget)
                .maxOutputTokens(m.maxOutputTokens)
                .temperature(m.temperature)
                .model(m.model)
                .build()
        }

        registerBean<ChatClient>("chatClient", primary = true) {
            val m = bean<GenAIProperties>().model
            val options = bean<ChatOptions>("chatOptions")

            bean<ChatClient.Builder>()
                .defaultSystem("${m.systemPrompt}\n${ResponseData.converter.instruction}")
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .defaultAdvisors(SimpleLoggerAdvisor())
                .defaultOptions(options)
                .defaultTools(*bean<AIToolArray>().tools)
                .build()
        }

        registerBean<ChatOptions>("searchChatOptions") {
            val m = bean<GenAIProperties>().searchTool

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
                .thinkingBudget(m.thinkingBudget)
                .maxOutputTokens(m.maxOutputTokens)
                .temperature(m.temperature)
                .model(m.model)
                .build()
        }

        registerBean<ChatClient>("searchChatClient") {
            val options = bean<ChatOptions>("searchChatOptions")
            val m = bean<GenAIProperties>().searchTool

            bean<ChatClient.Builder>()
                .defaultOptions(options)
                .defaultSystem(m.systemPrompt)
                .build()
        }
    }
})
