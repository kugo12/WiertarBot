package pl.kvgx12.wiertarbot.telegram

import korlibs.encoding.base64
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import pl.kvgx12.telegram.TelegramWebhook
import pl.kvgx12.telegram.data.Update
import pl.kvgx12.wiertarbot.connector.ConnectorBeanRegistrar
import pl.kvgx12.wiertarbot.telegram.TelegramProperties.Companion.BASE_URL_PROPERTY
import pl.kvgx12.wiertarbot.telegram.TelegramProperties.Companion.USE_NEW_CONNECTOR_PROPERTY
import java.security.SecureRandom

@ConfigurationProperties("wiertarbot.telegram")
data class TelegramProperties(
    val token: String,
    val useNewConnector: Boolean = false,
    val baseUrl: String? = null,
) {
    companion object {
        const val WEBHOOK_PATH = "/api/telegram/webhook"

        const val BASE_URL_PROPERTY = "wiertarbot.telegram.base-url"
        const val USE_NEW_CONNECTOR_PROPERTY = "wiertarbot.telegram.use-new-connector"
    }
}

@SpringBootApplication(proxyBeanMethods = false)
@Import(ConnectorBeanRegistrar::class, BeansRegistrar::class)
@EnableConfigurationProperties(TelegramProperties::class)
class Application

class BeansRegistrar : BeanRegistrarDsl({
    if (env.getProperty(USE_NEW_CONNECTOR_PROPERTY)?.toBoolean() == true) {
        registerBean<TelegramKtConnector>()
        registerBean<TelegramKtContext>()
    } else {
        registerBean<TelegramConnector>()
        registerBean<TelegramContext>()
    }

    if (!env.getProperty(BASE_URL_PROPERTY).isNullOrEmpty()) {
        registerBean {
            val props = bean<TelegramProperties>()

            val byteArray = ByteArray(64)
            SecureRandom.getInstanceStrong().nextBytes(byteArray)

            TelegramWebhook(bean(), props.baseUrl!! + TelegramProperties.WEBHOOK_PATH, byteArray.base64)
        }
        registerBean {
            val webhook = bean<TelegramWebhook>()

            coRouter {
                POST(TelegramProperties.WEBHOOK_PATH) { request ->
                    webhook.handleUpdate(
                        getHeader = request.headers()::firstHeader,
                        body = { request.awaitBody<Update>() },
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
