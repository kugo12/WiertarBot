package pl.kvgx12.wiertarbot.commands

import com.google.genai.Client
import com.google.genai.types.*
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
    private val client: Client.Async = Client.builder()
        .vertexAI(false)
        .apiKey(props.apiKey)
        .build()
        .async

    private val contentConfig = GenerateContentConfig.builder().run {
        responseMimeType("text/plain")
        maxOutputTokens(props.maxOutputTokens)
        temperature(props.temperature)

        systemInstruction(
            Content.builder().run {
                parts(
                    Part.fromText(
                        XML.encodeToString(SystemPrompt(props.systemPrompt)),
                    ),
                )
                build()
            },
        )
        thinkingConfig(
            ThinkingConfig.builder().run {
                includeThoughts(false)
                thinkingBudget(props.thinkingBudget)
                build()
            },
        )
        tools(
            buildList {
                if (props.grounding) {
                    add(
                        Tool.builder()
                            .googleSearch(GoogleSearch.builder().build())
                            .build(),
                    )
                }
            },
        )
        build()
    }

    suspend fun generate(prompt: String): String {
        return client.models.generateContent(
            props.model,
            XML.encodeToString(UserPrompt(prompt)),
            contentConfig,
        ).await().text() ?: TODO()
    }
}
