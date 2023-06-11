package pl.kvgx12.downloadapi.services

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.*
import aws.sdk.kotlin.services.s3.model.MetadataDirective
import aws.sdk.kotlin.services.s3.model.NotFound
import aws.sdk.kotlin.services.s3.model.S3Exception
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.content.FileContent
import aws.smithy.kotlin.runtime.net.Url
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import java.io.File

@ConfigurationProperties("storage.s3")
data class S3Properties(
    val region: String = "auto",
    val forcePathStyle: Boolean = true,
    val url: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val bucket: String,
)

@EnableConfigurationProperties(S3Properties::class)
class S3Service(private val properties: S3Properties) {
    private val client = S3Client {
        region = properties.region
        endpointUrl = Url.parse(properties.url)
        forcePathStyle = properties.forcePathStyle

        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = properties.accessKeyId
            secretAccessKey = properties.secretAccessKey
        }
    }

    suspend fun upload(name: String, data: ByteStream) = client.putObject {
        bucket = properties.bucket
        key = name
        body = data
    }

    suspend fun upload(name: String, file: File) = upload(name, FileContent(file))

    suspend fun exists(name: String) = try {
        client.headObject {
            bucket = properties.bucket
            key = name
        }
        true
    } catch (_: NotFound) {
        false
    }

    suspend fun bumpModificationTime(name: String, ifUnmodifiedSince: Instant? = null) = try {
        client.copyObject {
            bucket = properties.bucket
            key = name
            copySource = "${properties.bucket}/$name"
            metadata = mapOf("_ts" to Clock.System.now().epochSeconds.toString())
            metadataDirective = MetadataDirective.Replace
            copySourceIfUnmodifiedSince = ifUnmodifiedSince?.toJavaInstant()?.let {
                aws.smithy.kotlin.runtime.time.Instant(it)
            }
        }
        Unit
    } catch (_: S3Exception) {
    }
}
