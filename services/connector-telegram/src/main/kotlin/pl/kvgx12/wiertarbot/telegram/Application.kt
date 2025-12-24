package pl.kvgx12.wiertarbot.telegram

import kotlinx.serialization.json.Json
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import pl.kvgx12.telegram.TelegramClient
import pl.kvgx12.telegram.TelegramWebhook
import pl.kvgx12.telegram.data.TUpdate
import pl.kvgx12.wiertarbot.connector.ConnectorBeanRegistrar
import pl.kvgx12.wiertarbot.telegram.TelegramProperties.Companion.BASE_URL_PROPERTY
import java.security.SecureRandom

@ConfigurationProperties("wiertarbot.telegram")
data class TelegramProperties(
    val token: String,
    val baseUrl: String? = null,
) {
    companion object {
        const val WEBHOOK_PATH = "/api/telegram/webhook"

        const val BASE_URL_PROPERTY = "wiertarbot.telegram.base-url"
    }
}

@SpringBootApplication(proxyBeanMethods = false)
@Import(ConnectorBeanRegistrar::class, BeansRegistrar::class)
@EnableConfigurationProperties(TelegramProperties::class)
class Application

class BeansRegistrar : BeanRegistrarDsl({
    registerBean {
        TelegramClient(bean<TelegramProperties>().token)
    }
    registerBean<TelegramKtConnector>()
    registerBean<TelegramKtContext>()

    registerBean {
        object : WebFluxConfigurer {
            override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
                val json = Json {
                    ignoreUnknownKeys = true
                }
                configurer.defaultCodecs().kotlinSerializationJsonDecoder(KotlinSerializationJsonDecoder(json))
                configurer.defaultCodecs().kotlinSerializationJsonEncoder(KotlinSerializationJsonEncoder(json))
            }
        }
    }

    if (!env.getProperty(BASE_URL_PROPERTY).isNullOrEmpty()) {
        registerBean {
            val props = bean<TelegramProperties>()

            val byteArray = ByteArray(64)
            SecureRandom.getInstanceStrong().nextBytes(byteArray)
            val token = byteArray.joinToString(separator = "") { "%02x".format(it) }

            TelegramWebhook(bean(), props.baseUrl!! + TelegramProperties.WEBHOOK_PATH, token)
        }
        registerBean {
            val webhook = bean<TelegramWebhook>()

            coRouter {
                POST(TelegramProperties.WEBHOOK_PATH) { request ->
                    webhook.handleUpdate(
                        getHeader = request.headers()::firstHeader,
                        body = { request.awaitBody<TUpdate>() },
                    )

                    ok().buildAndAwait()
                }
            }
        }
    }
})

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        setWebApplicationType(WebApplicationType.REACTIVE)
    }
}
