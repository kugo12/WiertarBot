package pl.kvgx12.downloadapi.platforms.facebook

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import it.skrape.core.htmlDocument
import it.skrape.selects.html5.div
import kotlinx.serialization.json.Json
import pl.kvgx12.downloadapi.platform.*
import pl.kvgx12.downloadapi.platform.FileExtensions.MP4
import pl.kvgx12.downloadapi.utils.FindUrlWithKeyTraversal
import pl.kvgx12.downloadapi.utils.HostPredicate
import pl.kvgx12.downloadapi.utils.Url
import pl.kvgx12.downloadapi.utils.base64
import java.io.File

class FacebookPlatform : Platform {
    override val name: String get() = "facebook"

    override suspend fun tryParsing(url: Url): UrlMetadata? {
        if (!isFacebookUrl(url)) return null

        return EmptyMetadata(url)
    }

    override suspend fun download(allocator: ResourceAllocator, url: Url, metadata: UrlMetadata): Media {
        val file = allocator.allocateTempFile(url.base64(), MP4)

        return tryDownload(file, url)
            ?: tryDownload(file, Platform.followRedirects(url))
            ?: error("Unable to download video from $url")
    }

    private suspend fun tryDownload(file: File, url: Url): Media? =
        if (isMCapable(url)) {
            val body = Platform.client.get(Url(url) {
                host = "m.facebook.com"
            }).bodyAsText()

            val jsonData = htmlDocument(body) {
                div {
                    withAttribute = "data-sigil" to "inlineVideo"

                    findFirst {
                        attribute("data-store")
                    }
                }
            }

            val mediaUrl = traversal.traverse(Json.decodeFromString(jsonData)).first()

            Platform.getFile(mediaUrl, file)

            file.asVideo()
        } else null

    private fun isMCapable(url: Url) = mCapablePredicate.test(url.host)
    private fun isFacebookUrl(url: Url) = hostPredicate.test(url.host)

    companion object {
        private val hostPredicate = HostPredicate("fb.watch", "facebook.com", "fb.me", "fb.com")
        private val mCapablePredicate = HostPredicate("facebook.cok")
        private val traversal = FindUrlWithKeyTraversal("src")
    }
}
