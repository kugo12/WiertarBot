package pl.kvgx12.wiertarbot.command

import pl.kvgx12.wiertarbot.config.WiertarbotProperties

data class HelpEvaluationContext(
    val prefix: String,
    val name: String,
    val aliases: String,
) {
    inline fun StringBuilder.usage(string: String = "") =
        section("Użycie")
            .append(prefix).append(name).append(' ').append(string)

    inline fun StringBuilder.returns(string: String) =
        section("Zwraca", string)

    inline fun StringBuilder.info(string: String) =
        section("Informacja", string)

    inline fun StringBuilder.section(name: String) =
        append(name).append(":\n    ")

    inline fun StringBuilder.section(name: String, value: String) =
        section(name)
            .append(value)

    companion object {
        inline fun from(dsl: CommandDsl) = HelpEvaluationContext(
            dsl.dsl.ref<WiertarbotProperties>().prefix,
            dsl.name!!,
            dsl.aliases.joinToString(", ")
        )
    }
}

typealias HelpEval = HelpEvaluationContext.(StringBuilder) -> StringBuilder
