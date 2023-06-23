package pl.kvgx12.downloadapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DownloadApiApplication

fun main(args: Array<String>) {
    runApplication<DownloadApiApplication>(*args) {
        addInitializers(beans)
    }
}
