package pl.kvgx12.wiertarbot.config

import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.cache.caffeine.CaffeineCacheManager
import pl.kvgx12.wiertarbot.QueueConsumer
import pl.kvgx12.wiertarbot.services.*


class BeanRegistrar : BeanRegistrarDsl({
    registerBean { CaffeineCacheManager("permissions") }


    registerBean("rabbitListenerContainerFactory") {
        SimpleRabbitListenerContainerFactory().apply<SimpleRabbitListenerContainerFactory> {
            setConnectionFactory(bean())
            setAcknowledgeMode(AcknowledgeMode.AUTO)
        }
    }

    registerBean<RabbitMQ>()
    registerBean<PermissionDecoderService>()
    registerBean<PermissionService>()

    registerBean<CommandRegistrationService>()
    registerBean<CommandService>()
    registerBean<GrpcConfig>()

    registerBean<CacheService>()
    registerBean<CachedContextService>()

    if ("test" !in env.activeProfiles) {
        registerBean<QueueConsumer>()
    }
})
