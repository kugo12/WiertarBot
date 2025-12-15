package pl.kvgx12.wiertarbot.commands

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import pl.kvgx12.wiertarbot.commands.ai.GenAICommandsRegistrar
import pl.kvgx12.wiertarbot.commands.clients.ClientBeansRegistrar
import pl.kvgx12.wiertarbot.commands.image.edit.ImageEditCommandsRegistrar
import pl.kvgx12.wiertarbot.commands.image.random.LocalMediaCommandsRegistrar
import pl.kvgx12.wiertarbot.commands.image.random.RandomImageApiCommandsRegistrar
import pl.kvgx12.wiertarbot.commands.image.random.RandomImageScrapingCommandsRegistrar
import pl.kvgx12.wiertarbot.commands.standard.StandardCommandsRegistrar

@Configuration
@Import(
    ClientBeansRegistrar::class,
    SpecialCommandsRegistrar::class,
    ImageEditCommandsRegistrar::class,
    StandardCommandsRegistrar::class,
    UtilityCommandsRegistrar::class,
    GenAICommandsRegistrar::class,
    LocalMediaCommandsRegistrar::class,
    RandomImageApiCommandsRegistrar::class,
    RandomImageScrapingCommandsRegistrar::class,
    TTRSCommandsRegistrar::class,
    DownloadCommandRegistrar::class,
    WeatherCommandRegistrar::class,
)
class CommandBeans
