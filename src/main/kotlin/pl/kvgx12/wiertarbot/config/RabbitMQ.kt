package pl.kvgx12.wiertarbot.config

import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.support.BeanDefinitionDsl

fun BeanDefinitionDsl.rabbitBeans() {
    bean {
        RabbitTemplate(ref()).apply {
            setExchange(ref<WiertarbotProperties>().rabbitMQExchange)
        }
    }
}