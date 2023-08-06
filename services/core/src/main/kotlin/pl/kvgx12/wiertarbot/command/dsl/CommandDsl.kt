package pl.kvgx12.wiertarbot.command.dsl

import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.proto.ConnectorType
import java.util.*

class CommandDsl(
    val dsl: BeanDefinitionDsl.BeanSupplierContext,
    val name: String,
    val aliases: List<String> = emptyList(),
) {
    var help: String? = null
    var availableIn: EnumSet<ConnectorType> = EnumSet.allOf(ConnectorType::class.java)

    inline fun help(eval: HelpEval) {
        help = eval(
            HelpEvaluationContext.from(this),
            StringBuilder(),
        ).toString()
    }

    fun help(usage: String = "", returns: String? = null, info: String? = null) {
        help { builder ->
            builder.apply {
                usage(usage)
                returns?.let { returns(it) }
                info?.let { info(it) }
            }
        }
    }
}
