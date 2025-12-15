package pl.kvgx12.wiertarbot.telegram

import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import pl.kvgx12.wiertarbot.connector.ConnectorBeanRegistrar

@SpringBootApplication(proxyBeanMethods = false)
@Import(ConnectorBeanRegistrar::class, BeansRegistrar::class)
class Application

class BeansRegistrar : BeanRegistrarDsl({
    registerBean<TelegramConnector>()
    registerBean<TelegramContext>()
})

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        setWebApplicationType(WebApplicationType.NONE)
    }
}
