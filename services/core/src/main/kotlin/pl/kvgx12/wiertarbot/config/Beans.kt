package pl.kvgx12.wiertarbot.config

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySource
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.support.beans
import org.springframework.core.env.ConfigurableEnvironment
import pl.kvgx12.wiertarbot.QueueConsumer
import pl.kvgx12.wiertarbot.commands.commandBeans
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.services.*
import kotlin.reflect.full.findAnnotation

fun beans() = beans {
    val binder = env.getBinder()

    bean { binder }
    bean { CaffeineCacheManager("permissions") }

    bean { binder.bind<WiertarbotProperties>() }
    bean { binder.bind<GrpcProperties>() }
    rabbitBeans()

    bean<RabbitMQ>()
    bean<PermissionDecoderService>()
    bean<PermissionService>()

    commandBeans()
    bean<CommandRegistrationService>()
    bean<CommandService>()
    bean<GrpcConfig>()

    if (!env.activeProfiles.contains("test")) {
        bean<QueueConsumer>()
    }
}

inline fun <reified T : Any> ConfigurableEnvironment.get(key: String, default: T): T =
    getProperty(key, T::class.java, default)

fun ConfigurableEnvironment.getBinder() = Binder(propertySources.mapNotNull { ConfigurationPropertySource.from(it) })

inline fun <reified T : Any> Binder.bind(): T = bindOrCreate(
    T::class.findAnnotation<ConfigProperties>()!!.value,
    T::class.java,
)

annotation class ConfigProperties(val value: String)
