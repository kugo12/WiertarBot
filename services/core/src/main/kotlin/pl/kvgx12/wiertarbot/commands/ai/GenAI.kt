package pl.kvgx12.wiertarbot.commands.ai

import com.google.genai.Client
import com.google.genai.types.GoogleSearch
import com.google.genai.types.HarmBlockThreshold
import com.google.genai.types.HarmCategory
import kotlinx.coroutines.future.await
import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties

@ConfigurationProperties("wiertarbot.genai")
data class GenAIProperties(
    val apiKey: String,
    val model: String,
    val systemPrompt: String,
    val thinkingBudget: Int? = 1024,
    val maxOutputTokens: Int = 2048,
    val temperature: Float = 1f,
    val grounding: Boolean = true,
)

@Serializable
data class SystemPrompt(@XmlElement val instruction: String)

@Serializable
data class UserPrompt(@XmlElement val prompt: String)

@EnableConfigurationProperties(GenAIProperties::class)
class GenAI(
    private val props: GenAIProperties,
) {
    private val client: Client.Async = Client(props.apiKey).async

    private val contentConfig = generateContentConfig {
        responseMimeType("text/plain")
        maxOutputTokens(props.maxOutputTokens)
        temperature(props.temperature)

        systemInstruction {
            text(XML.encodeToString(SystemPrompt(props.systemPrompt)).also { println(it) })
        }

        thinkingConfig {
            includeThoughts(true)
            thinkingBudget(props.thinkingBudget)
        }

        tools {
            if (props.grounding) {
                tool {
                    googleSearch(GoogleSearch.builder().build())
                }
            }
        }

        // probably by default they're off, so just to be sure
        safetySettings {
            for (category in HarmCategory.Known.entries) {
                add {
                    category(category)
                    threshold(HarmBlockThreshold.Known.OFF)
                }
            }
        }
    }

    suspend fun generate(prompt: String): String {
        val content = content {
            text(XML.encodeToString(UserPrompt(prompt)))
        }

        val result = client.models.generateContent(props.model, content, contentConfig)
            .await()

        val text = result.text() ?: TODO()

        return text
    }
}
