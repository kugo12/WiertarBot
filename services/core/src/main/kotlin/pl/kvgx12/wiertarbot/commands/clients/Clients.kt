package pl.kvgx12.wiertarbot.commands.clients

import kotlinx.serialization.json.Json
import org.reactivestreams.Publisher
import org.springframework.context.support.BeanDefinitionDsl
import org.springframework.core.ResolvableType
import org.springframework.core.codec.Decoder
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.util.MimeType
import org.springframework.util.MimeTypeUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient
import pl.kvgx12.wiertarbot.commands.clients.external.*
import pl.kvgx12.wiertarbot.commands.clients.internal.DownloadClient
import pl.kvgx12.wiertarbot.commands.clients.internal.TTRSClient
import pl.kvgx12.wiertarbot.commands.clients.internal.WeatherClient
import pl.kvgx12.wiertarbot.config.properties.DownloadApiProperties
import pl.kvgx12.wiertarbot.config.properties.TTRSProperties
import pl.kvgx12.wiertarbot.config.properties.WeatherProperties
import pl.kvgx12.wiertarbot.utils.KiB
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

fun BeanDefinitionDsl.clientBeans() {
    bean(isPrimary = true) {
        WebClient.builder()
            .codecs { it.defaultCodecs().kotlinSerializationJsonDecoder(JsonDecoder) }
            .build()
    }

    bean(isPrimary = true) {
        HttpServiceProxyFactory.builderFor(
            WebClientAdapter.create(ref()),
        ).build()
    }

    httpClient<ImgFlipClient>()
    httpClient<UnsplashApiClient>()

    httpClient<CurrencyApiClient>()
    bean<CurrencyApi>()

    httpClient<AliPaczkaClient>()
    bean<AliPaczka>()

    httpClient<MojangApiClient>()
    bean<Minecraft>()

    httpClient<SjpPwnClient> {
        codecs {
            it.defaultCodecs().maxInMemorySize(512 * KiB)
        }
    }
    bean<SjpPwn>()

    httpClient<SucharClient>()
    bean<Suchar>()

    httpClient<PLCovidStatsClient>()
    bean<PLCovidStats>()

    httpClient<MiejskiClient>()
    bean<Miejski>()

    httpClient<FantanoClient>()
    bean<Fantano>()

    bean {
        provider<DownloadApiProperties>().ifAvailable {
            val connector = ReactorClientHttpConnector(
                reactor.netty.http.client.HttpClient.create()
                    .responseTimeout(Duration.ofMinutes(10)),
            )

            httpClient<DownloadClient> {
                baseUrl(it.url)
                clientConnector(connector)
            }
        }

        provider<TTRSProperties>().ifAvailable {
            httpClient<TTRSClient> { baseUrl(it.url) }
        }

        provider<WeatherProperties>().ifAvailable {
            httpClient<WeatherClient> { baseUrl(it.url) }
        }
    }
}

inline fun <reified T : Any> BeanDefinitionDsl.httpClient() =
    bean { ref<HttpServiceProxyFactory>().createClient<T>() }

inline fun <reified T : Any> BeanDefinitionDsl.httpClient(crossinline builder: WebClient.Builder.() -> Unit) =
    bean {
        HttpServiceProxyFactory.builderFor(
            WebClientAdapter.create(
                WebClient.builder()
                    .apply<_>(builder)
                    .build(),
            ),
        ).build().createClient<T>()
    }

// FIXME
// ugly workaround :c
private object JsonDecoder : Decoder<Any> {
    private val delegate = KotlinSerializationJsonDecoder(
        Json {
            ignoreUnknownKeys = true
        },
    )
    private val textJson = MimeType("text", "json")
    private val decodableMimeTypes = delegate.decodableMimeTypes + textJson

    private fun overrideMimeType(mimeType: MimeType?) =
        if (mimeType == textJson) MimeTypeUtils.APPLICATION_JSON else mimeType

    override fun canDecode(elementType: ResolvableType, mimeType: MimeType?) =
        delegate.canDecode(elementType, overrideMimeType(mimeType))

    override fun decode(
        inputStream: Publisher<DataBuffer>,
        elementType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Flux<Any> = delegate.decode(inputStream, elementType, mimeType, hints)

    override fun decodeToMono(
        inputStream: Publisher<DataBuffer>,
        elementType: ResolvableType,
        mimeType: MimeType?,
        hints: MutableMap<String, Any>?
    ): Mono<Any> = delegate.decodeToMono(inputStream, elementType, mimeType, hints)

    override fun getDecodableMimeTypes() = decodableMimeTypes
}
