package pl.kvgx12.wiertarbot.connector

import io.grpc.BindableService
import io.grpc.ServerBuilder
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.support.BeanDefinitionDsl

fun BeanDefinitionDsl.connectorBeans() {
    bean<RabbitMQ>()
    bean<Runner>()
    bean<DelegatedCommandInvoker>()
    bean<ConnectorConfiguration>()
}

@ConfigurationProperties("wiertarbot.connector")
data class ConnectorProperties(val port: Int = 8080)

@EnableConfigurationProperties(ConnectorProperties::class)
class ConnectorConfiguration(private val props: ConnectorProperties) {
    @Bean
    fun connectorServer(services: List<BindableService>) =
        ServerBuilder.forPort(props.port)
            .addServices(services.map { it.bindService() })
            .build()
}
