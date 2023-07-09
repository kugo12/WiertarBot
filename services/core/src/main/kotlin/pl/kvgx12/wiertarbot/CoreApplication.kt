package pl.kvgx12.wiertarbot

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import pl.kvgx12.wiertarbot.repositories.PermissionRepository

@EnableScheduling
@EnableAsync
@EnableCaching
@SpringBootApplication(proxyBeanMethods = false, scanBasePackageClasses = [PermissionRepository::class])
class CoreApplication

fun main(args: Array<String>) {
    runApplication<CoreApplication>(*args) {
        webApplicationType = WebApplicationType.NONE
    }
}
