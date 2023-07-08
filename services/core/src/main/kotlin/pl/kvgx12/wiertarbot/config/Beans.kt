package pl.kvgx12.wiertarbot.config

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySource
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.support.beans
import org.springframework.core.env.ConfigurableEnvironment
import pl.kvgx12.wiertarbot.Runner
import pl.kvgx12.wiertarbot.commands.commandBeans
import pl.kvgx12.wiertarbot.config.properties.FBProperties
import pl.kvgx12.wiertarbot.config.properties.TelegramProperties
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.connectors.fb.FBKtConnector
import pl.kvgx12.wiertarbot.connectors.fb.FBKtEventConsumer
import pl.kvgx12.wiertarbot.connectors.fb.FBKtMilestoneTracker
import pl.kvgx12.wiertarbot.connectors.fb.FBMessageService
import pl.kvgx12.wiertarbot.connectors.telegram.TelegramConnector
import pl.kvgx12.wiertarbot.services.*
import kotlin.reflect.full.findAnnotation

fun beans() = beans {
    val binder = env.getBinder()

    bean { binder }
    bean { CaffeineCacheManager("permissions") }

    bean { binder.bind<WiertarbotProperties>() }
    rabbitBeans()

    bean<RabbitMQService>()
    bean<PermissionDecoderService>()
    bean<PermissionService>()

    commandBeans()
    bean<CommandRegistrationService>()
    bean<CommandService>()

    if (env.get("wiertarbot.fb.enabled", false)) {
        bean { binder.bind<FBProperties>() }
        bean<FBMessageService>()

        bean<FBKtEventConsumer>()
        bean<FBKtConnector>()
        bean<FBKtMilestoneTracker>()
    }

    if (env.get("wiertarbot.telegram.enabled", false)) {
        bean { binder.bind<TelegramProperties>() }
        bean<TelegramConnector>()
    }

    if (!env.activeProfiles.contains("test")) bean<Runner>()
}

inline fun <reified T : Any> ConfigurableEnvironment.get(key: String, default: T): T =
    getProperty(key, T::class.java, default)

fun ConfigurableEnvironment.getBinder() = Binder(propertySources.mapNotNull { ConfigurationPropertySource.from(it) })

inline fun <reified T : Any> Binder.bind(): T = bindOrCreate(
    T::class.findAnnotation<ConfigProperties>()!!.value,
    T::class.java,
)

annotation class ConfigProperties(val value: String)
