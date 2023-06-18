package pl.kvgx12.wiertarbot.config

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties

fun BeanDefinitionDsl.rabbitBeans() {
    bean {
        RabbitTemplate(ref()).apply {
            setExchange(ref<WiertarbotProperties>().rabbitMQExchange)
        }
    }
}
