package pl.kvgx12.downloadapi

import io.ktor.http.*
import org.springframework.context.support.beans
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

val beans
    get() = beans {
        bean<RedditPlatform>()
        bean<YoutubePlatform>()
        bean<TikTokPlatform>()
        bean<TwitterPlatform>()
        bean<InstagramPlatform>()
        bean<FacebookPlatform>()
        bean<StreamablePlatform>()

        bean<SignatureService>()
        bean<S3Service>()
        bean<DownloadService>()

        bean(::router)
    }

fun router(
    downloadService: DownloadService,
) = coRouter {
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
