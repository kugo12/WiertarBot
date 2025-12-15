package pl.kvgx12.downloadapi

import io.ktor.http.*
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import pl.kvgx12.downloadapi.platforms.facebook.FacebookPlatform
import pl.kvgx12.downloadapi.platforms.instagram.InstagramPlatform
import pl.kvgx12.downloadapi.platforms.reddit.RedditPlatform
import pl.kvgx12.downloadapi.platforms.streamable.StreamablePlatform
import pl.kvgx12.downloadapi.platforms.tiktok.TikTokPlatform
import pl.kvgx12.downloadapi.platforms.twitter.TwitterPlatform
import pl.kvgx12.downloadapi.platforms.youtube.YoutubePlatform
import pl.kvgx12.downloadapi.services.DownloadService
import pl.kvgx12.downloadapi.services.S3Service
import pl.kvgx12.downloadapi.services.SignatureService
import pl.kvgx12.downloadapi.utils.isUrl

class BeanRegistrar : BeanRegistrarDsl({
    registerBean<RedditPlatform>()
    registerBean<YoutubePlatform>()
    registerBean<TikTokPlatform>()
    registerBean<TwitterPlatform>()
    registerBean<InstagramPlatform>()
    registerBean<FacebookPlatform>()
    registerBean<StreamablePlatform>()

    registerBean<SignatureService>()
    registerBean<S3Service>()
    registerBean<DownloadService>()

    registerBean {
        router(bean())
    }
})

fun router(downloadService: DownloadService) = coRouter {
    val urlParam = queryParam("url", String::isUrl)

    GET("/", urlParam) {
        val url = Url(it.queryParam("url").get())

        when (val presignedUrl = downloadService.download(url)) {
            null -> unprocessableEntity().buildAndAwait()
            else -> ok().bodyValueAndAwait(presignedUrl)
        }
    }

    GET("/platforms") { ok().bodyValueAndAwait(downloadService.supportedPlatformNames) }
}
