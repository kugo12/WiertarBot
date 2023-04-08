package pl.kvgx12.wiertarbot

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableAsync
@EnableCaching
@SpringBootApplication(proxyBeanMethods = false)
class CoreApplication

fun main(args: Array<String>) {
	runApplication<CoreApplication>(*args) {
		webApplicationType = WebApplicationType.NONE
	}
}
