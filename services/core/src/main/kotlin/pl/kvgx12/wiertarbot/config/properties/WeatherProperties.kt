package pl.kvgx12.wiertarbot.config.properties

import pl.kvgx12.wiertarbot.config.ConfigProperties

@ConfigProperties("wiertarbot.weather")
data class WeatherProperties(val url: String)
