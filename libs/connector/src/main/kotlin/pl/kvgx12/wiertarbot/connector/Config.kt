package pl.kvgx12.wiertarbot.connector

import io.grpc.BindableService
import io.grpc.Server
import io.grpc.ServerBuilder
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import pl.kvgx12.wiertarbot.connector.utils.getLogger

class ConnectorBeanRegistrar : BeanRegistrarDsl({
    registerBean<RabbitMQ>()
    registerBean<Runner>()
    registerBean<DelegatedCommandInvoker>()
    registerBean<ConnectorConfiguration>()
})

@ConfigurationProperties("wiertarbot.connector")
data class ConnectorProperties(val port: Int = 8080)

@EnableConfigurationProperties(ConnectorProperties::class)
class ConnectorConfiguration(private val props: ConnectorProperties) {
    private val log = getLogger()

    @Bean
    fun connectorServer(services: List<BindableService>): Server {
        log.info("Starting Connector gRPC server on port ${props.port}")

        return ServerBuilder.forPort(props.port)
            .intercept(GrpcExceptionInterceptor())
            .addServices(services.map { it.bindService() })
            .build()
    }
}
