package pl.kvgx12.wiertarbot.commands

import com.google.genai.Client
import com.google.genai.types.*
import kotlinx.coroutines.future.await
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.command.dsl.text


val genAICommand = commands {
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
                parts(listOf(Part.fromText(props.systemPrompt)))
                build()
            },
        )
        thinkingConfig(
            ThinkingConfig.builder().run {
                includeThoughts(props.thinkingBudget != null)
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

    suspend fun generate(prompt: String): String = client.models.generateContent(
        props.model,
        prompt,
        contentConfig,
    ).await().text() ?: TODO()
}
