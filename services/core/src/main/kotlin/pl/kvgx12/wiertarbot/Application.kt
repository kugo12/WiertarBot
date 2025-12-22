package pl.kvgx12.wiertarbot

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableAsync
import pl.kvgx12.wiertarbot.commands.CommandBeans
import pl.kvgx12.wiertarbot.config.BeanRegistrar
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties

@EnableAsync
@EnableCaching
@SpringBootConfiguration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ConfigurationPropertiesScan(basePackageClasses = [WiertarbotProperties::class])
@Import(BeanRegistrar::class, CommandBeans::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        setWebApplicationType(WebApplicationType.NONE)
    }
}
