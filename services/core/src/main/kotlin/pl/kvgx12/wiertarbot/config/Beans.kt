package pl.kvgx12.wiertarbot.config

import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.ConfigurationPropertySource
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.support.beans
import org.springframework.core.env.ConfigurableEnvironment
import pl.kvgx12.wiertarbot.Runner
import pl.kvgx12.wiertarbot.commands.commandBeans
import pl.kvgx12.wiertarbot.config.properties.FBProperties
import pl.kvgx12.wiertarbot.config.properties.PythonSentryProperties
import pl.kvgx12.wiertarbot.config.properties.TelegramProperties
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.connectors.fb.*
import pl.kvgx12.wiertarbot.connectors.telegram.TelegramConnector
import pl.kvgx12.wiertarbot.services.*
import kotlin.reflect.full.findAnnotation

annotation class ConfigProperties(val value: String)

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

        if (env.get("wiertarbot.fb.new-connector", false)) {
            bean<FBKtEventConsumer>()
            bean<FBKtConnector>()
            bean<FBKtMilestoneTracker>()
        } else {
            bean<FBConnector>()
            interpreterBeans()
            if (env.getProperty("wiertarbot.sentry.python") != null) {
                bean { binder.bind<PythonSentryProperties>() }
            }
        }
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

inline fun <reified T : Any> Binder.bind(): T = bindOrCreate(
    T::class.findAnnotation<ConfigProperties>()!!.value,
    T::class.java,
)
