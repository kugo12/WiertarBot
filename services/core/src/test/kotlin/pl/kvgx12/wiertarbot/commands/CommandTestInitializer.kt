package pl.kvgx12.wiertarbot.commands

import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.text
import pl.kvgx12.wiertarbot.config.bind
import pl.kvgx12.wiertarbot.config.getBinder
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.services.CommandRegistrationService
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.utils.proto.set
import pl.kvgx12.wiertarbot.utils.scopedCommandName

class CommandTestInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(applicationContext: GenericApplicationContext) {
        beans {
            val binder = env.getBinder()

            bean { binder }
            bean { binder.bind<WiertarbotProperties>() }
            bean {
                mockk<PermissionService> {
                    coEvery { isAuthorized(any(), any(), any()) } returns true
                    coEvery { initPermissionByCommand(any()) } returns Unit
                }
            }
            bean<CommandRegistrationService>()

            commandBeans()

            ConnectorType.entries.forEach {
                command(it.scopedCommandName()) {
                    availableIn = it.set()
                    help(returns = "io io $it")

                    text { ":v" }
                }
            }
        }.initialize(applicationContext)
    }
}
