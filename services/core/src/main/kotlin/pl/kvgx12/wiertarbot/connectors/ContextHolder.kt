package pl.kvgx12.wiertarbot.connectors

import pl.kvgx12.wiertarbot.connector.ConnectorContext
import pl.kvgx12.wiertarbot.proto.ConnectorType
import java.util.*


object ContextHolder {
    private val contexts: MutableMap<ConnectorType, ConnectorContext> = Collections.synchronizedMap(mutableMapOf())

    fun get(type: ConnectorType): ConnectorContext =
        contexts[type] ?: throw IllegalStateException("No context for $type")

    fun set(type: ConnectorType, context: ConnectorContext) {
        contexts[type] = context
    }
}
