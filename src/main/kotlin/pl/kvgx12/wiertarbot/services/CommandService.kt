package pl.kvgx12.wiertarbot.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import pl.kvgx12.wiertarbot.Constants
import pl.kvgx12.wiertarbot.command.ImageEditCommand
import pl.kvgx12.wiertarbot.config.WiertarbotProperties
import pl.kvgx12.wiertarbot.events.MessageEvent
import java.time.Instant
import java.util.*
import kotlin.collections.set

class CommandService(
    private val permissionService: PermissionService,
    private val wiertarbotProperties: WiertarbotProperties,
    commandRegistrationService: CommandRegistrationService,
) {
    private val commands = commandRegistrationService.commands
    private val special = commandRegistrationService.specialCommands
    private val aliases = commandRegistrationService.aliases

    private val imageEditQueue = Collections.synchronizedMap(
        mutableMapOf<String, Pair<Long, ImageEditCommand.ImageEditState>>()
    )
    private val specialCommandsContext = Dispatchers.Default + SupervisorJob()

    suspend fun dispatch(event: MessageEvent) = coroutineScope {
        if (event.authorId == event.context.getBotId() || permissionService.isAuthorized(
                "banned",
                event.threadId,
                event.authorId
            )
        )
            return@coroutineScope

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
                        event.authorId
                    )
                ) {
                    when (val command = commands[commandName]) {
                        is CommandHandler.ImageEdit -> launch {
                            command(event)?.let {
                                imageEditQueue[event.editQueueId] = Instant.now().epochSecond to it
                            }
                        }

                        is CommandHandler.KtGeneric -> launch {
                            command(event)?.send()
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
                    if (edit.tryEditAndSend(event))
                        imageEditQueue.remove(queueId)
                }
            } else imageEditQueue.remove(queueId)
        }
    }

    private inline val MessageEvent.editQueueId get() = "${threadId}_${authorId}"
}
