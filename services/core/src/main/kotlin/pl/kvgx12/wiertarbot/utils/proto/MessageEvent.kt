package pl.kvgx12.wiertarbot.utils.proto

import pl.kvgx12.wiertarbot.connectors.ContextHolder
import pl.kvgx12.wiertarbot.proto.MessageEvent


val MessageEvent.isGroup get() = threadId != authorId
suspend fun MessageEvent.react(reaction: String) = context.reactToMessage(this, reaction)
val MessageEvent.context get() = ContextHolder.get(connectorInfo.connectorType)
