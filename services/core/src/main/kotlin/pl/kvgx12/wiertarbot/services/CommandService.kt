package pl.kvgx12.wiertarbot.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.command.GenericCommandHandler
import pl.kvgx12.wiertarbot.command.ImageEditCommand
import pl.kvgx12.wiertarbot.command.SpecialCommand
import pl.kvgx12.wiertarbot.config.properties.WiertarbotProperties
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.utils.proto.context
import pl.kvgx12.wiertarbot.utils.proto.send
import java.time.Instant
import java.util.*
import kotlin.collections.set

class CommandService(
    private val permissionService: PermissionService,
    private val wiertarbotProperties: WiertarbotProperties,
    private val special: List<SpecialCommand>,
    commandRegistrationService: CommandRegistrationService,
) {
    private val commands = commandRegistrationService.commandsByConnector
    private val aliases = commandRegistrationService.aliases

    private val imageEditQueue = Collections.synchronizedMap(
        mutableMapOf<String, Pair<Long, ImageEditCommand.ImageEditState>>(),
    )
    private val specialCommandsContext = Dispatchers.Default + SupervisorJob()

    suspend fun dispatch(event: MessageEvent) = coroutineScope {
        if (event.authorId == event.connectorInfo.botId || permissionService.isAuthorized(
                "banned",
                event.threadId,
                event.authorId,
            )
        ) {
            return@coroutineScope
        }

        if (event.text.isNotEmpty()) {
            if (event.text.startsWith(wiertarbotProperties.prefix)) {
                val commandName = event.text
                    .substringBefore(' ')
                    .drop(wiertarbotProperties.prefix.length)
                    .lowercase()
                    .let(aliases::get)

                if (commandName != null && permissionService.isAuthorized(
                        commandName,
                        event.threadId,
                        event.authorId,
                    )
                ) {
                    when (val command = commands[event.context.connectorType]!![commandName]?.handler) {
                        is ImageEditCommand -> launch {
                            command.check(event)?.let {
                                imageEditQueue[event.editQueueId] = Instant.now().epochSecond to it
                            }
                        }

                        is GenericCommandHandler -> launch {
                            command.process(event)?.send()
                        }

                        null -> {}
                    }
                }
            }

            launch(specialCommandsContext) {
                special.forEach { it.process(event) }
            }
        } else {
            val queueId = event.editQueueId
            val (time, edit) = imageEditQueue[queueId] ?: return@coroutineScope

            if (time + Constants.imageEditTimeout > Instant.now().epochSecond) {
                launch {
                    if (edit.tryEditAndSend(event)) {
                        imageEditQueue.remove(queueId)
                    }
                }
            } else imageEditQueue.remove(queueId)
        }
    }

    private inline val MessageEvent.editQueueId get() = "${threadId}_$authorId"
}
