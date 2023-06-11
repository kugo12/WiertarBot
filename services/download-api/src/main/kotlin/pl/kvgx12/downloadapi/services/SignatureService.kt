package pl.kvgx12.downloadapi.services

import io.ktor.http.*
import io.ktor.util.*
import kotlinx.datetime.Clock
import org.apache.commons.codec.digest.HmacAlgorithms
import org.apache.commons.codec.digest.HmacUtils
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import pl.kvgx12.downloadapi.utils.Url
import kotlin.time.Duration

@ConfigurationProperties("signature")
data class SignatureProperties(
    val key: String,
    val baseUrl: String,
    val expires: String = "8h",
) {
    val expiresDuration = Duration.parse(expires)
}

@EnableConfigurationProperties(SignatureProperties::class)
class SignatureService(private val properties: SignatureProperties) {
    private val hmac = HmacUtils(HmacAlgorithms.HMAC_SHA_256, properties.key)

    val expires = properties.expiresDuration

    fun sign(name: String) = Url(properties.baseUrl) {
        pathSegments = name.split('/')

        val expires = Clock.System.now().plus(expires).toEpochMilliseconds()
        parameters["e"] = expires.toString()
        parameters["sig"] = hmac.hmac("/$name@$expires").encodeBase64()
    }
}
