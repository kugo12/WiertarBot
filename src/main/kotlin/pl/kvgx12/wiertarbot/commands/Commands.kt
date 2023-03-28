package pl.kvgx12.wiertarbot.commands

import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.commands.image.edit.imageEditCommands
import pl.kvgx12.wiertarbot.commands.image.random.localMediaCommands
import pl.kvgx12.wiertarbot.commands.image.random.randomImageApiCommands
import pl.kvgx12.wiertarbot.commands.image.random.randomImageScrapingCommands

val commandBeans = commands {
    specialCommands()
    imageEditCommands()
    utilityCommands()
    localMediaCommands()
    randomImageApiCommands()
    randomImageScrapingCommands()
}
