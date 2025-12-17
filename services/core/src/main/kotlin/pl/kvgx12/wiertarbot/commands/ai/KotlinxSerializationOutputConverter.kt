package pl.kvgx12.wiertarbot.commands.ai

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.springframework.ai.converter.*
import pl.kvgx12.wiertarbot.utils.getLogger

@OptIn(ExperimentalSerializationApi::class)
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private val cleaner = CompositeResponseTextCleaner.builder()
    .addCleaner(WhitespaceCleaner())
    .addCleaner(ThinkingTagCleaner())
    .addCleaner(MarkdownCodeBlockCleaner())
    .addCleaner(WhitespaceCleaner())
    .build();

class KotlinxSerializationOutputConverter<T : Any>(
    private val serializer: KSerializer<T>,
) : StructuredOutputConverter<T> {
    val schema = buildString {
        appendJsonSchema(serializer.descriptor)
    }

    // copied from BeanOutputConverter
    private val format = """
        Your response should be in JSON format.
        Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.
        Do not include markdown code blocks in your response.
        Remove the ```json markdown from the output.
        Here is the JSON Schema instance your output must adhere to:
        $schema
    """.trimIndent()

    companion object {
        private val log = getLogger()
    }

    override fun getFormat(): String = format

    override fun convert(raw: String): T {
        val cleaned = cleaner.clean(raw)

        log.debug("Cleaned response: {}", cleaned)

        return json.decodeFromString(serializer, cleaned)
    }

    private fun StringBuilder.appendJsonSchema(descriptor: SerialDescriptor) {
        append("{\"type\": \"object\", \"properties\": {")

        for (index in 0 until descriptor.elementsCount) {
            if (index > 0) append(", ")
            val name = descriptor.getElementName(index)
            val elementDescriptor = descriptor.getElementDescriptor(index)
            append("\"$name\": ")
            appendJsonType(elementDescriptor)
        }

        append("}, \"required\": [")

        var first = true
        for (index in 0 until descriptor.elementsCount) {
            if (!descriptor.isElementOptional(index)) {
                if (!first) append(", ")
                first = false
                append("\"${descriptor.getElementName(index)}\"")
            }
        }

        append("]}")
    }

    private fun StringBuilder.appendJsonType(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            PrimitiveKind.STRING -> append("{\"type\": \"string\"}")
            PrimitiveKind.INT,
            PrimitiveKind.LONG,
            PrimitiveKind.SHORT,
            PrimitiveKind.BYTE -> append("{\"type\": \"integer\"}")

            PrimitiveKind.FLOAT,
            PrimitiveKind.DOUBLE -> append("{\"type\": \"number\"}")

            PrimitiveKind.BOOLEAN -> append("{\"type\": \"boolean\"}")
            StructureKind.LIST -> {
                val itemDescriptor = descriptor.getElementDescriptor(0)
                append("{\"type\": \"array\", \"items\": ")
                appendJsonType(itemDescriptor)
                append("}")
            }

            StructureKind.CLASS,
            StructureKind.OBJECT -> appendJsonSchema(descriptor)

            else -> append("{\"type\": \"string\"}")
        }
    }

}

inline fun <reified T : Any> kotlinxOutputConverter(): KotlinxSerializationOutputConverter<T> {
    return KotlinxSerializationOutputConverter(serializer<T>())
}
