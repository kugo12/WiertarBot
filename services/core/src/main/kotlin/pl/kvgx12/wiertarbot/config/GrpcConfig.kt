package pl.kvgx12.wiertarbot.config

import io.grpc.ManagedChannelBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import pl.kvgx12.wiertarbot.connector.ConnectorContextClient
import pl.kvgx12.wiertarbot.proto.ConnectorInfo
import pl.kvgx12.wiertarbot.proto.ConnectorType

@ConfigurationProperties("wiertarbot.grpc")
data class GrpcProperties(
    val clients: Map<ConnectorType, Client>,
) {
    data class Client(
        val url: String,
    )
}

@EnableConfigurationProperties(GrpcProperties::class)
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
        checkNotNull(clients[key]) { "No context for $key" }

    operator fun get(key: ConnectorInfo): ConnectorContextClient =
        checkNotNull(clients[key.connectorType]) { "No context for $key" }
}
