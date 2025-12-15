package pl.kvgx12.wiertarbot.commands.ai

import com.google.genai.Client
import com.google.genai.types.*

@DslMarker
annotation class GenAIDsl

@GenAIDsl
fun Client(key: String): Client = Client.builder()
    .vertexAI(false)
    .apiKey(key)
    .build()

@GenAIDsl
inline fun content(role: String? = null, func: MutableList<Part>.() -> Unit): Content =
    Content.builder()
        .apply { if (role != null) role(role) }
        .parts(buildList(func))
        .build()

@GenAIDsl
fun MutableList<Part>.text(text: String) {
    add(Part.fromText(text))
}

@GenAIDsl
inline fun generateContentConfig(func: GenerateContentConfig.Builder.() -> Unit): GenerateContentConfig =
    GenerateContentConfig.builder().apply(func).build()

@GenAIDsl
inline fun GenerateContentConfig.Builder.systemInstruction(func: MutableList<Part>.() -> Unit) {
    systemInstruction(content(func = func))
}

@GenAIDsl
inline fun GenerateContentConfig.Builder.thinkingConfig(func: ThinkingConfig.Builder.() -> Unit) {
    thinkingConfig(
        ThinkingConfig.builder()
            .apply(func)
            .build(),
    )
}

@GenAIDsl
inline fun GenerateContentConfig.Builder.safetySettings(func: MutableList<SafetySetting>.() -> Unit) {
    safetySettings(buildList(func))
}

@GenAIDsl
inline fun MutableList<SafetySetting>.add(func: SafetySetting.Builder.() -> Unit) {
    add(
        SafetySetting.builder()
            .apply(func)
            .build(),
    )
}

@GenAIDsl
fun GenerateContentConfig.Builder.tools(func: MutableList<Tool>.() -> Unit) {
    tools(buildList(func))
}

@GenAIDsl
inline fun MutableList<Tool>.tool(func: Tool.Builder.() -> Unit) {
    add(
        Tool.builder()
            .apply(func)
            .build(),
    )
}
