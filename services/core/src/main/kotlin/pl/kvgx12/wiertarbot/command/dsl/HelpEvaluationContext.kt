package pl.kvgx12.wiertarbot.command.dsl

import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties

data class HelpEvaluationContext(
    val prefix: String,
    val name: String,
    val aliases: String,
) {
    fun StringBuilder.usage(string: String = ""): StringBuilder =
        section("Użycie")
            .append(prefix).append(name).append(' ').append(string).append('\n')

    fun StringBuilder.additionalUsage(string: String): StringBuilder =
        append("    ").append(prefix).append(name).append(' ').append(string).append('\n')

    fun StringBuilder.returns(string: String): StringBuilder =
        section("Zwraca", string)

    fun StringBuilder.info(string: String): StringBuilder =
        section("Informacja", string)

    fun StringBuilder.section(name: String): StringBuilder =
        append(name).append(":\n    ")

    fun StringBuilder.section(name: String, value: String): StringBuilder =
        section(name)
            .append(value)
            .append('\n')

    companion object {
        fun from(dsl: CommandDsl) = HelpEvaluationContext(
            dsl.dsl.bean<WiertarbotProperties>().prefix,
            dsl.name,
            dsl.aliases.joinToString(", "),
        )
    }
}

typealias HelpEval = HelpEvaluationContext.(StringBuilder) -> StringBuilder
