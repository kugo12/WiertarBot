package pl.kvgx12.wiertarbot.commands

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans
import pl.kvgx12.wiertarbot.command.dsl.command
import pl.kvgx12.wiertarbot.command.dsl.text
import pl.kvgx12.wiertarbot.config.ContextHolder
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.connector.ConnectorContextClient
import pl.kvgx12.wiertarbot.proto.ConnectorInfo
import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.services.CommandRegistrationService
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.utils.proto.set
import pl.kvgx12.wiertarbot.utils.scopedCommandName

class CommandTestInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(applicationContext: GenericApplicationContext) {
        beans {
            bean {
                @ConfigurationPropertiesScan(basePackageClasses = [WiertarbotProperties::class])
                object {}
            }

            bean {
                mockk<PermissionService> {
                    coEvery { isAuthorized(any(), any(), any()) } returns true
                    coEvery { initPermissionByCommand(any()) } returns Unit
                }
            }
            bean<CommandRegistrationService>()

            bean { mockk<ConnectorContextClient>() }

            bean {
                val client = ref<ConnectorContextClient>()

                mockk<ContextHolder> {
                    every { get(any<ConnectorType>()) } returns client
                    every { get(any<ConnectorInfo>()) } returns client
                }
            }

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
