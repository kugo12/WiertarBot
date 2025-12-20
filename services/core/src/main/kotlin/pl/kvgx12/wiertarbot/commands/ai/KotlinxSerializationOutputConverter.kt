package pl.kvgx12.wiertarbot.commands.ai

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.springframework.ai.converter.CompositeResponseTextCleaner
import org.springframework.ai.converter.MarkdownCodeBlockCleaner
import org.springframework.ai.converter.ThinkingTagCleaner
import org.springframework.ai.converter.WhitespaceCleaner
import pl.kvgx12.wiertarbot.utils.appendJsonSchema

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private val cleaner = CompositeResponseTextCleaner.builder()
    .addCleaner(WhitespaceCleaner())
    .addCleaner(ThinkingTagCleaner())
    .addCleaner(MarkdownCodeBlockCleaner())
    .addCleaner {
        when {
            it.startsWith('{') -> it
            '{' in it -> '{' + it.substringAfter('{')
            else -> "{\"text\":\"$it"
        }
    }
    .addCleaner {
        when {
            it.endsWith('}') -> it
            it.endsWith('"') -> "$it}"
            else -> "$it\"}"
        }
    }
    .build()


class KotlinxSerializationOutputConverter<T : Any>(private val serializer: KSerializer<T>) {
    // copied from BeanOutputConverter

    val schema = buildString {
        appendJsonSchema(serializer.descriptor)
    }
    val instruction = """
        Your response should be in JSON format.
        Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
        Do not include markdown code blocks in your response.
        Remove the ```json markdown from the output.
        Here is the JSON Schema instance your output must adhere to:
        $schema
        """.trimIndent()

    fun clean(raw: String): String = cleaner.clean(raw)

    fun convert(raw: String): T = json.decodeFromString(serializer, raw)
}

inline fun <reified T : Any> kotlinxOutputConverter(): KotlinxSerializationOutputConverter<T> {
    return KotlinxSerializationOutputConverter(serializer<T>())
}
