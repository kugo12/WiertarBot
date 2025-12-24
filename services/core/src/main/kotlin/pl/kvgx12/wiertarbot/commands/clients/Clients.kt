package pl.kvgx12.wiertarbot.commands.clients

import kotlinx.serialization.json.Json
import org.reactivestreams.Publisher
import org.springframework.beans.factory.BeanRegistrarDsl
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
import pl.kvgx12.wiertarbot.commands.clients.internal.CEXApi
import pl.kvgx12.wiertarbot.commands.clients.internal.CEXClient
import pl.kvgx12.wiertarbot.commands.clients.internal.DownloadClient
import pl.kvgx12.wiertarbot.commands.clients.internal.TTRSClient
import pl.kvgx12.wiertarbot.config.properties.CEXProperties
import pl.kvgx12.wiertarbot.config.properties.DownloadApiProperties
import pl.kvgx12.wiertarbot.config.properties.TTRSProperties
import pl.kvgx12.wiertarbot.utils.KiB
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

class ClientBeansRegistrar : BeanRegistrarDsl({
    registerBean(primary = true) {
        WebClient.builder()
            .codecs { it.defaultCodecs().kotlinSerializationJsonDecoder(JsonDecoder) }
            .build()
    }

    registerBean(primary = true) {
        HttpServiceProxyFactory.builderFor(
            WebClientAdapter.create(bean()),
        ).build()
    }

    registerHttpClient<ImgFlipClient>()
    registerHttpClient<UnsplashApiClient>()

    registerHttpClient<CurrencyApiClient>()
    registerBean<CurrencyApi>()

    registerHttpClient<AliPaczkaClient>()
    registerBean<AliPaczka>()

    registerHttpClient<WeatherComApi>()
    registerBean<WeatherComClient>()

    registerHttpClient<MojangApiClient>()
    registerBean<Minecraft>()

    registerHttpClient<SjpPwnClient> {
        codecs {
            it.defaultCodecs().maxInMemorySize(512 * KiB)
        }
    }
    registerBean<SjpPwn>()

    registerHttpClient<SucharClient>()
    registerBean<Suchar>()

    registerHttpClient<PLCovidStatsClient>()
    registerBean<PLCovidStats>()

    registerHttpClient<MiejskiClient>()
    registerBean<Miejski>()

    registerHttpClient<FantanoClient>()
    registerBean<Fantano>()

    if (env.getProperty("wiertarbot.download-api.url") != null) {
        registerHttpClient<DownloadClient> {
            val connector = ReactorClientHttpConnector(
                reactor.netty.http.client.HttpClient.create()
                    .responseTimeout(Duration.ofMinutes(10)),
            )

            baseUrl(it.bean<DownloadApiProperties>().url)
            clientConnector(connector)
        }
    }

    if (env.getProperty("wiertarbot.ttrs.url") != null) {
        registerHttpClient<TTRSClient> {
            baseUrl(it.bean<TTRSProperties>().url)
        }
    }

    if (env.getProperty("wiertarbot.cex.api-key") != null) {
        registerHttpClient<CEXApi> {
            baseUrl(it.bean<CEXProperties>().url)
        }
        registerBean<CEXClient>()
    }
})

inline fun <reified T : Any> BeanRegistrarDsl.registerHttpClient() =
    registerBean { bean<HttpServiceProxyFactory>().createClient<T>() }

inline fun <reified T : Any> BeanRegistrarDsl.registerHttpClient(crossinline builder: WebClient.Builder.(BeanRegistrarDsl.SupplierContextDsl<*>) -> Unit) =
    registerBean {
        HttpServiceProxyFactory.builderFor(
            WebClientAdapter.create(
                WebClient.builder()
                    .apply<_> {
                        builder(this, this@registerBean)
                    }
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
