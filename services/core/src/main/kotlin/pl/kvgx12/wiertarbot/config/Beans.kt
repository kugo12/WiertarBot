package pl.kvgx12.wiertarbot.config

import io.r2dbc.spi.ConnectionFactory
import org.springframework.amqp.core.AcknowledgeMode
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions
import org.springframework.data.r2dbc.dialect.DialectResolver
import pl.kvgx12.wiertarbot.QueueConsumer
import pl.kvgx12.wiertarbot.entities.ByteArrayToMediaWrapperConverter
import pl.kvgx12.wiertarbot.entities.MediaWrapperToByteArrayConverter
import pl.kvgx12.wiertarbot.repositories.PostgresCacheRepository
import pl.kvgx12.wiertarbot.services.*


class BeanRegistrar : BeanRegistrarDsl({
    registerBean { CaffeineCacheManager("permissions") }


    registerBean("rabbitListenerContainerFactory") {
        SimpleRabbitListenerContainerFactory().apply<SimpleRabbitListenerContainerFactory> {
            setConnectionFactory(bean())
            setAcknowledgeMode(AcknowledgeMode.AUTO)
        }
    }

    registerBean {
        val dialect = DialectResolver.getDialect(bean<ConnectionFactory>())
        R2dbcCustomConversions.of(
            dialect,
            listOf(
                MediaWrapperToByteArrayConverter(),
                ByteArrayToMediaWrapperConverter(),
            )
        )
    }

    registerBean<PostgresCacheRepository>()

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
