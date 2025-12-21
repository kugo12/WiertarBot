package pl.kvgx12.wiertarbot.commands

import org.springframework.beans.factory.BeanRegistrarDsl
import pl.kvgx12.wiertarbot.commands.ai.GenAIRegistrar
import pl.kvgx12.wiertarbot.commands.clients.ClientBeansRegistrar
import pl.kvgx12.wiertarbot.commands.image.edit.ImageEditCommandsRegistrar
import pl.kvgx12.wiertarbot.commands.image.random.LocalMediaCommandsRegistrar
import pl.kvgx12.wiertarbot.commands.image.random.RandomImageApiCommandsRegistrar
import pl.kvgx12.wiertarbot.commands.image.random.RandomImageScrapingCommandsRegistrar
import pl.kvgx12.wiertarbot.commands.standard.StandardCommandsRegistrar

class CommandBeans : BeanRegistrarDsl({
    register(ClientBeansRegistrar())
    register(SpecialCommandsRegistrar())
    register(ImageEditCommandsRegistrar())
    register(StandardCommandsRegistrar())
    register(UtilityCommandsRegistrar())
    register(GenAIRegistrar())
    register(LocalMediaCommandsRegistrar())
    register(RandomImageApiCommandsRegistrar())
    register(RandomImageScrapingCommandsRegistrar())
    register(TTRSCommandsRegistrar())
    register(DownloadCommandRegistrar())

    weatherCommand()
})
