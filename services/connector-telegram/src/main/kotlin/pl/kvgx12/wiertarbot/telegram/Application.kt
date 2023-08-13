package pl.kvgx12.wiertarbot.telegram

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.support.beans
import pl.kvgx12.wiertarbot.connector.connectorBeans

@SpringBootApplication(proxyBeanMethods = false)
class Application

fun beans() = beans {
    bean<TelegramConnector>()
    bean<TelegramContext>()

    connectorBeans()
}

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        webApplicationType = WebApplicationType.NONE
        addInitializers(beans())
    }
}
