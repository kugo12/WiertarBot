package pl.kvgx12.wiertarbot.commands

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.BeanRegistrarDsl
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
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


@ConfigurationPropertiesScan(basePackageClasses = [WiertarbotProperties::class])
@Import(CommandTestBeanRegistrar::class, CommandBeans::class)
@Configuration
class CommandTestConfiguration

class CommandTestBeanRegistrar : BeanRegistrarDsl({
    registerBean {
        mockk<PermissionService> {
            coEvery { isAuthorized(any(), any(), any()) } returns true
            coEvery { initPermissionByCommand(any()) } returns Unit
        }
    }
    registerBean<CommandRegistrationService>()

    registerBean {
        val client = bean<ConnectorContextClient>()

        mockk<ContextHolder> {
            every { get(any<ConnectorType>()) } returns client
            every { get(any<ConnectorInfo>()) } returns client
        }
    }

    ConnectorType.entries.forEach {
        command(it.scopedCommandName()) {
            availableIn = it.set()
            help(returns = "io io $it")

            text { ":v" }
        }
    }
})
