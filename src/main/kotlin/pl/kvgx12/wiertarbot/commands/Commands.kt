package pl.kvgx12.wiertarbot.commands

import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.commands.image.edit.imageEditCommands
import pl.kvgx12.wiertarbot.commands.image.random.localMediaCommands
import pl.kvgx12.wiertarbot.commands.image.random.randomImageApiCommands
import pl.kvgx12.wiertarbot.commands.image.random.randomImageScrapingCommands
import pl.kvgx12.wiertarbot.commands.standard.standardCommands

val commandBeans = commands {
    specialCommands()

    imageEditCommands()

    standardCommands()
    utilityCommands()

    localMediaCommands()
    randomImageApiCommands()
    randomImageScrapingCommands()
}
