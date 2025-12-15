package pl.kvgx12.wiertarbot.commands.image.random

import org.springframework.beans.factory.BeanRegistrarDsl
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.file
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries

class LocalMediaCommandsRegistrar : BeanRegistrarDsl({
    localMediaCommand("jabol", "jabola", "random/jabol")
    localMediaCommand("mikser", "miksera", "random/mikser")
    localMediaCommand("audi", "audi", "random/audi")
    localMediaCommand("bmw", "bmw", "random/bmw")
    localMediaCommand("papaj", "papieża", "random/papaj")
    localMediaCommand("konon", "konona", "random/konon")
})

private fun BeanRegistrarDsl.localMediaCommand(name: String, returns: String, dir: String) =
    command(name) {
        help(returns = "losowe zdjęcie $returns")

        val files = (Constants.commandMediaPath / dir).listDirectoryEntries()

        file { files.random().toString() }
    }
