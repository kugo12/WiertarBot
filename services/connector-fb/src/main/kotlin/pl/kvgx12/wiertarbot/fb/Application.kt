package pl.kvgx12.wiertarbot.fb

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import pl.kvgx12.wiertarbot.fb.config.FBProperties
import pl.kvgx12.wiertarbot.fb.config.beans

@EnableScheduling
@EnableAsync
@EnableCaching
@SpringBootApplication(proxyBeanMethods = false)
@EnableConfigurationProperties(FBProperties::class)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        webApplicationType = WebApplicationType.NONE
        addInitializers(beans())
    }
}
