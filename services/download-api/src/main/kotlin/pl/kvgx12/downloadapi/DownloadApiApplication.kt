package pl.kvgx12.downloadapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(BeanRegistrar::class)
class DownloadApiApplication

fun main(args: Array<String>) {
    runApplication<DownloadApiApplication>(*args)
}
