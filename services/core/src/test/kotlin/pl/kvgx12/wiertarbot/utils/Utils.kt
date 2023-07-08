package pl.kvgx12.wiertarbot.utils

import io.mockk.FunctionMatcher
import org.springframework.beans.factory.getBean
import org.springframework.context.support.GenericApplicationContext
import pl.kvgx12.wiertarbot.command.Command
import pl.kvgx12.wiertarbot.connector.ConnectorType
import pl.kvgx12.wiertarbot.events.Response

fun GenericApplicationContext.getCommand(name: String) = getBean<Command>(name)

fun ConnectorType.scopedCommandName() = "$name-test-only"

fun responseTextMatcher(text: String) = FunctionMatcher<Response>(
    {
        it.text == text
    },
    Response::class,
)
