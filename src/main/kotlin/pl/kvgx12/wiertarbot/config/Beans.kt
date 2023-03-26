package pl.kvgx12.wiertarbot.config

import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySource
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.support.beans
import org.springframework.core.env.ConfigurableEnvironment
import pl.kvgx12.wiertarbot.Runner
import pl.kvgx12.wiertarbot.commands.commandBeans
import pl.kvgx12.wiertarbot.connectors.FBConnector
import pl.kvgx12.wiertarbot.connectors.TelegramConnector
import pl.kvgx12.wiertarbot.services.*
import kotlin.reflect.full.findAnnotation

annotation class ConfigProperties(val value: String)

fun beans() = beans {
    val binder = env.getBinder()

    bean { CaffeineCacheManager("permissions") }

    bean { binder.bind<WiertarbotProperties>() }
    interpreterBeans()
    rabbitBeans()

    bean<RabbitMQService>()
    bean<FBMessageService>()
    bean<PermissionDecoderService>()
    bean<PermissionService>()

    commandBeans()
    bean<CommandRegistrationService>()
    bean<CommandService>()

    if (env.get("wiertarbot.fb.enabled", false)) {
        bean<FBConnector>()
    }

    if (env.get("wiertarbot.telegram.enabled", false)) {
        bean { binder.bind<TelegramProperties>() }
        bean<TelegramConnector>()
    }

    if (!env.activeProfiles.contains("test")) bean<Runner>()
}

inline fun <reified T : Any> ConfigurableEnvironment.get(key: String, default: T) =
    getProperty(key, T::class.java, default)

fun ConfigurableEnvironment.getBinder() = Binder(propertySources.mapNotNull { ConfigurationPropertySource.from(it) })

inline fun <reified T : Any> Binder.bind(): T =
    bind(
        T::class.findAnnotation<ConfigProperties>()!!.value,
        Bindable.of(T::class.java)
    ).get()
