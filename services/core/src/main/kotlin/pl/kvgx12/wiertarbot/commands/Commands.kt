package pl.kvgx12.wiertarbot.commands

import org.springframework.boot.context.properties.bind.Binder
import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.commands.image.edit.imageEditCommands
import pl.kvgx12.wiertarbot.commands.image.random.localMediaCommands
import pl.kvgx12.wiertarbot.commands.image.random.randomImageApiCommands
import pl.kvgx12.wiertarbot.commands.image.random.randomImageScrapingCommands
import pl.kvgx12.wiertarbot.commands.standard.standardCommands
import pl.kvgx12.wiertarbot.config.bind
import pl.kvgx12.wiertarbot.config.properties.DownloadApiProperties
import pl.kvgx12.wiertarbot.config.properties.TTRSProperties
import pl.kvgx12.wiertarbot.config.properties.WeatherProperties

val commandBeans = commands {
    specialCommands()

    imageEditCommands()

    standardCommands()
    utilityCommands()

    localMediaCommands()
    randomImageApiCommands()
    randomImageScrapingCommands()

    if (env.getProperty("wiertarbot.ttrs.url") != null) {
        bean { ref<Binder>().bind<TTRSProperties>() }
        ttrsCommands()
    }

    if (env.getProperty("wiertarbot.download-api.url") != null) {
        bean { ref<Binder>().bind<DownloadApiProperties>() }
        downloadCommand()
    }

    if (env.getProperty("wiertarbot.weather.url") != null) {
        bean { ref<Binder>().bind<WeatherProperties>() }

        weatherCommand()
    }
}
