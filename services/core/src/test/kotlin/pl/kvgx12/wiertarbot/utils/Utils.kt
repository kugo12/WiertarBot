package pl.kvgx12.wiertarbot.utils

import io.mockk.FunctionMatcher
import org.springframework.beans.factory.getBean
import org.springframework.context.support.GenericApplicationContext
import pl.kvgx12.wiertarbot.command.CommandMetadata
import pl.kvgx12.wiertarbot.command.GenericCommandHandler
import pl.kvgx12.wiertarbot.proto.ConnectorType
import pl.kvgx12.wiertarbot.proto.Response

fun GenericApplicationContext.getCommand(name: String) = getBean<CommandMetadata>(name).let {
    it to it.handler as GenericCommandHandler
}

fun ConnectorType.scopedCommandName() = "$name-test-only"

fun responseTextMatcher(text: String) = FunctionMatcher<Response>(
    {
        it.text == text
    },
    Response::class,
)
