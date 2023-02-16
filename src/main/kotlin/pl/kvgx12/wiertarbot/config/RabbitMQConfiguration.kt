package pl.kvgx12.wiertarbot.config

import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfiguration {
    @Bean
    fun rabbitTemplate(wiertarbotProperties: WiertarbotProperties, connectionFactory: ConnectionFactory) =
        RabbitTemplate(connectionFactory).apply {
            setExchange(wiertarbotProperties.rabbitMQExchange)
        }
}