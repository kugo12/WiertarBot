package pl.kvgx12.downloadapi.platform

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import it.skrape.core.htmlDocument
import it.skrape.selects.html5.meta
import it.skrape.selects.html5.title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import pl.kvgx12.downloadapi.utils.io
import pl.kvgx12.downloadapi.utils.isUrl
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption

interface Platform {
    val name: String

    suspend fun tryParsing(url: Url): UrlMetadata?
    suspend fun download(allocator: ResourceAllocator, url: Url, metadata: UrlMetadata): Media

    companion object {
        val client = HttpClient(Java) {
            expectSuccess = true

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }

            install(HttpTimeout)
            install(HttpCookies)
            install(Resources)

            BrowserUserAgent()

            defaultRequest {
                accept(ContentType.Any)
            }

            engine {
                protocolVersion = java.net.http.HttpClient.Version.HTTP_2
            }
        }

        val log: Logger = LoggerFactory.getLogger(Platform::class.java)

        suspend fun followRedirects(url: Url): Url {
            val response = client.get(url)
            val headUrl = response.request.url

            return if (headUrl == url) {
                val body = response.bodyAsText()
                val refreshUrl = htmlDocument(body) {
                    relaxed = true

                    val title = title {
                        findFirst {
                            html
                        }
                    }

                    if (title.isUrl) {
                        Url(title)
                    } else {
                        meta {
                            withAttribute = "http-equiv" to "refresh"

                            findFirst {
                                val content = attributes["content"]
                                    ?.split("url=", ignoreCase = true, limit = 2)
                                    ?.last()
                                    .orEmpty()

                                if (content.isUrl) Url(content) else null
                            }
                        }
                    }
                }

                refreshUrl ?: headUrl
            } else {
                headUrl
            }
        }

        private fun partsFactory(length: Long) = length.div(256 * KiB).coerceAtMost(50)

        suspend fun getFile(url: Url, file: File, builder: HttpRequestBuilder.() -> Unit = {}): ContentType? {
            val response = client.get(url) {
                timeout { requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS }
                builder()
            }
            log.info("${response.contentLength()}")
            response.bodyAsChannel().copyAndClose(file.writeChannel())

            return response.contentType()
        }

        suspend fun getFileParallel(
            url: Url,
            file: File,
            parts: (Long) -> Long = Companion::partsFactory,
        ): ContentType? {
            val head = client.head(url)
            val length = head.contentLength()
            checkNotNull(length)

            val scope = CoroutineScope(Dispatchers.IO)
            val fileChannel = io {
                FileChannel.open(
                    file.toPath(),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                )
            }

            (1 until length step length / parts(length)).toMutableList()
                .apply { if (last() != length - 1) add(length - 1) }.zipWithNext { a, b ->
                    scope.async {
                        client.prepareGet(url) {
                            timeout { requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS }
                            header(HttpHeaders.Range, "bytes=${a - 1}-$b")
                        }.execute {
                            val body = it.bodyAsChannel()
                            //                .joinTo(file.writeChannel(), true)

                            var position = a - 1
                            do {
                                body.read { source, _, _ ->
                                    fileChannel.write(source.buffer, position)
                                    position += source.size

                                    source.size32
                                }
                            } while (!body.isClosedForRead)

                            it.contentType()
                        }
                    }
                }.awaitAll()

            io { fileChannel.close() }

            return head.contentType()
        }
    }
}
