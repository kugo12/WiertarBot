package pl.kvgx12.wiertarbot.commands.image.random

import org.springframework.context.support.BeanDefinitionDsl
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.command.command
import pl.kvgx12.wiertarbot.command.commands
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries

val localMediaCommands = commands {
    localMediaCommand("jabol", "jabola", "random/jabol")
    localMediaCommand("mikser", "miksera", "random/mikser")
    localMediaCommand("audi", "audi", "random/audi")
    localMediaCommand("bmw", "bmw", "random/bmw")
    localMediaCommand("papaj", "papieża", "random/papaj")
    localMediaCommand("konon", "konona", "random/konon")
}

private fun BeanDefinitionDsl.localMediaCommand(name: String, returns: String, dir: String) =
    command(name) {
        help(returns = "losowe zdjęcie $returns")

        val files = (Constants.commandMediaPath / dir).listDirectoryEntries()

        files { listOf(files.random().toString()) }
    }
