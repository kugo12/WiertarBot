package pl.kvgx12.wiertarbot.config

import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.support.beans
import org.springframework.core.env.ConfigurableEnvironment
import pl.kvgx12.wiertarbot.QueueConsumer
import pl.kvgx12.wiertarbot.commands.commandBeans
import pl.kvgx12.wiertarbot.services.*

fun beans() = beans {
    bean { CaffeineCacheManager("permissions") }

    rabbitBeans()

    bean<RabbitMQ>()
    bean<PermissionDecoderService>()
    bean<PermissionService>()

    commandBeans()
    bean<CommandRegistrationService>()
    bean<CommandService>()
    bean<GrpcConfig>()

    if ("test" !in env.activeProfiles) {
        bean<QueueConsumer>()
    }
}

inline fun <reified T : Any> ConfigurableEnvironment.get(key: String, default: T): T =
    getProperty(key, T::class.java, default)
