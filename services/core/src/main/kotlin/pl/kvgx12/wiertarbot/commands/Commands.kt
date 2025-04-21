package pl.kvgx12.wiertarbot.commands

import pl.kvgx12.wiertarbot.command.dsl.commands
import pl.kvgx12.wiertarbot.commands.clients.clientBeans
import pl.kvgx12.wiertarbot.commands.image.edit.imageEditCommands
import pl.kvgx12.wiertarbot.commands.image.random.localMediaCommands
import pl.kvgx12.wiertarbot.commands.image.random.randomImageApiCommands
import pl.kvgx12.wiertarbot.commands.image.random.randomImageScrapingCommands
import pl.kvgx12.wiertarbot.commands.standard.standardCommands

val commandBeans = commands {
    clientBeans()

    specialCommands()

    imageEditCommands()

    standardCommands()
    utilityCommands()

    genAICommand()

    localMediaCommands()
    randomImageApiCommands()
    randomImageScrapingCommands()

    if (env.getProperty("wiertarbot.ttrs.url") != null) {
        ttrsCommands()
    }

    if (env.getProperty("wiertarbot.download-api.url") != null) {
        downloadCommand()
    }

    if (env.getProperty("wiertarbot.weather.url") != null) {
        weatherCommand()
    }
}
