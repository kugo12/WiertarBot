package pl.kvgx12.wiertarbot.commands

import pl.kvgx12.wiertarbot.command.commands
import pl.kvgx12.wiertarbot.commands.image.edit.imageEditCommands

val commandBeans = commands {
    specialCommands()
    imageEditCommands()
}
