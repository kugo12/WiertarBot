package pl.kvgx12.downloadapi.platforms.reddit

import io.ktor.resources.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Resource("/api/v1")
class RedditApi {
    @Resource("/access_token")
    data class AccessToken(val parent: RedditApi = RedditApi()) {
        @Serializable
        data class Response(
            @SerialName("access_token")
            val accessToken: String,
            @SerialName("token_type")
            val tokenType: String,
            @SerialName("expires_in")
            val expiresIn: Long,
            val scope: String,
        )
    }
}
