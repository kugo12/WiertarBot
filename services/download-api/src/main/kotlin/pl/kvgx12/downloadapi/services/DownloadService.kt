package pl.kvgx12.downloadapi.services

import io.ktor.http.*
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import pl.kvgx12.downloadapi.platform.*
import pl.kvgx12.downloadapi.utils.*
import kotlin.time.Duration.Companion.days

class DownloadService(
    private val platforms: List<Platform>,
    private val s3: S3Service,
    private val signatureService: SignatureService,
) {
    private val log = LoggerFactory.getLogger(DownloadService::class.java)
    private val downloadMutex = MutexHolder<String>()

    val supportedPlatformNames = platforms.map(Platform::name)

    suspend fun download(url: Url): String? {
        val (platform, metadata) = findPlatformBy(url) ?: return null

        val objectName = "${platform.name}/${metadata.filename}"

        downloadMutex.withLock(objectName) {
            if (!s3.exists(objectName)) {
                withResourceAllocator {
                    log.info("Downloading $url to $objectName")

                    s3.upload(
                        objectName,
                        download(platform, url, metadata)
                            .videoFile,
                    )
                }
            } else s3.bumpModificationTime(
                objectName,
                // TODO: remove hardcoded 7 days
                Clock.System.now() - 7.days + signatureService.expires,
            )
        }

        return signatureService.sign(objectName).toString()
    }

    private suspend fun ResourceAllocator.download(
        platform: Platform,
        url: Url,
        metadata: UrlMetadata,
    ): Media.Video =
        when (val media = platform.download(this, url, metadata)) {
            is Media.Video -> media
            is Media.VideoAndAudio -> media.mux(this)
        }

    private suspend fun findPlatformBy(url: Url) = platforms.firstNotNullOfOrNull { platform ->
        platform.tryParsing(url)?.let { metadata ->
            platform to metadata
        }
    }

    private suspend fun Media.VideoAndAudio.mux(allocator: ResourceAllocator): Media.Video {
        val output = allocator.allocateTempFile(
            videoFile.nameWithoutExtension + audioFile.nameWithoutExtension,
            ".mp4",
        )

        log.info("Muxing $audioFile + $videoFile -> $output")
        ffmpeg {
            overwriteOutput = true

            addInput(videoFile)
            addInput(audioFile)

            copyAudio()
            copyVideo()
            addOutput(output)
        }

        return Media.Video(output)
    }
}
