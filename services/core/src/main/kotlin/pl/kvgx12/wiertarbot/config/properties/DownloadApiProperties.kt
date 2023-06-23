package pl.kvgx12.wiertarbot.config.properties

import pl.kvgx12.wiertarbot.config.ConfigProperties

@ConfigProperties("wiertarbot.download-api")
data class DownloadApiProperties(
    val url: String,
) {
    val platformsUrl = "$url/platforms"
}
