package pl.kvgx12.wiertarbot.config

import io.grpc.ManagedChannelBuilder
import org.springframework.context.annotation.Bean
import pl.kvgx12.wiertarbot.connector.ConnectorContextClient
import pl.kvgx12.wiertarbot.proto.ConnectorInfo
import pl.kvgx12.wiertarbot.proto.ConnectorType

@ConfigProperties("wiertarbot.grpc")
data class GrpcProperties(
    val clients: Map<ConnectorType, Client>,
) {
    data class Client(
        val url: String,
    )
}

class GrpcConfig(private val props: GrpcProperties) {
    @Bean
    fun contextHolder() =
        props.clients.mapValues { (_, v) ->
            ManagedChannelBuilder.forTarget(v.url)
                .usePlaintext()
                .build()
                .let(::ConnectorContextClient)
        }.let(::ContextHolder)
}

class ContextHolder(private val clients: Map<ConnectorType, ConnectorContextClient>) {
    operator fun get(key: ConnectorType): ConnectorContextClient =
        clients[key] ?: throw IllegalStateException("No context for $key")

    operator fun get(key: ConnectorInfo): ConnectorContextClient =
        clients[key.connectorType] ?: throw IllegalStateException("No context for $key")
}
