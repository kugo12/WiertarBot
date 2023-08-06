package pl.kvgx12.wiertarbot.config

import io.grpc.BindableService
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import org.springframework.context.annotation.Bean
import pl.kvgx12.wiertarbot.connector.ConnectorContextClient
import pl.kvgx12.wiertarbot.proto.ConnectorInfo
import pl.kvgx12.wiertarbot.proto.ConnectorType

@ConfigProperties("wiertarbot.grpc")
data class GrpcProperties(
    val clients: Map<ConnectorType, Client>,
    val port: Int = 8080,
) {
    data class Client(
        val url: String,
    )
}

class GrpcConfig(
    private val props: GrpcProperties,
    private val services: List<BindableService>,
) {
    @Bean
    fun contextHolder() =
        props.clients.mapValues { (_, v) ->
            ManagedChannelBuilder.forTarget(v.url)
                .usePlaintext()
                .build()
                .let(::ConnectorContextClient)
        }.let(::ContextHolder)

    @Bean
    fun grpcServer(): Server =
        ServerBuilder.forPort(props.port)
            .addServices(services.map { it.bindService() })
            .build()
}

class ContextHolder(private val clients: Map<ConnectorType, ConnectorContextClient>) {
    operator fun get(key: ConnectorType): ConnectorContextClient =
        clients[key] ?: throw IllegalStateException("No context for $key")

    operator fun get(key: ConnectorInfo): ConnectorContextClient =
        clients[key.connectorType] ?: throw IllegalStateException("No context for $key")
}
