package pl.kvgx12.wiertarbot.config

import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.context.support.BeanDefinitionDsl

fun BeanDefinitionDsl.rabbitBeans() {
    bean("rabbitListenerContainerFactory") {
        SimpleRabbitListenerContainerFactory().apply {
            setConnectionFactory(ref())
            setAcknowledgeMode(AcknowledgeMode.MANUAL)
        }
    }

    bean {
        RabbitListenerConfigurer {
            it.setContainerFactory(ref())
        }
    }
}
