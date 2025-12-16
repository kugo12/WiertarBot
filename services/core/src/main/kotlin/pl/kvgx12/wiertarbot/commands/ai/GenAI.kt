package pl.kvgx12.wiertarbot.commands.ai

import kotlinx.coroutines.reactive.asFlow
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.google.genai.GoogleGenAiChatModel
import org.springframework.ai.google.genai.GoogleGenAiChatOptions
import org.springframework.ai.google.genai.common.GoogleGenAiSafetySetting
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties

@ConfigurationProperties("wiertarbot.genai")
data class GenAIProperties(
    val systemPrompt: String,
)

@EnableConfigurationProperties(GenAIProperties::class)
class GenAI(
    private val props: GenAIProperties,
    private val chat: GoogleGenAiChatModel,
) {
    private val systemMessage = SystemMessage(props.systemPrompt)
    private val chatOptions = GoogleGenAiChatOptions.builder()
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
        .build()

    suspend fun generate(prompt: String): String {
        val sb = StringBuilder()

        chat.stream(
            Prompt.builder()
                .chatOptions(chatOptions)
                .messages(
                    systemMessage,
                    UserMessage(prompt),
                )
                .build(),
        ).asFlow().collect {
            sb.append(it.result.output.text)
        }

        return sb.toString()
    }
}
