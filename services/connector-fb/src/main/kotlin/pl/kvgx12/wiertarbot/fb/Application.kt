package pl.kvgx12.wiertarbot.fb

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import pl.kvgx12.wiertarbot.connector.ConnectorBeanRegistrar
import pl.kvgx12.wiertarbot.fb.commands.DelegatedCommandsRegistrar
import pl.kvgx12.wiertarbot.fb.config.BeansRegistrar
import pl.kvgx12.wiertarbot.fb.config.FBProperties

@EnableScheduling
@EnableAsync
@EnableCaching
@SpringBootApplication(proxyBeanMethods = false)
@EnableConfigurationProperties(FBProperties::class)
@Import(DelegatedCommandsRegistrar::class, ConnectorBeanRegistrar::class, BeansRegistrar::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        setWebApplicationType(WebApplicationType.NONE)
    }
}
