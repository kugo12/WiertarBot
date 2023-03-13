package pl.kvgx12.wiertarbot

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = ["pl.kvgx12"], proxyBeanMethods = false)
@EnableScheduling
@EnableAsync
@EnableCaching
class CoreApplication

fun main(args: Array<String>) {
	runApplication<CoreApplication>(*args) {
		webApplicationType = WebApplicationType.NONE
	}
}
