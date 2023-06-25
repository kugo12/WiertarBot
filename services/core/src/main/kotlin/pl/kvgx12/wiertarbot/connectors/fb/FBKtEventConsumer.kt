package pl.kvgx12.wiertarbot.connectors.fb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionManager
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import org.springframework.transaction.support.TransactionTemplate
import pl.kvgx12.fbchat.data.events.Event
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.requests.react
import pl.kvgx12.fbchat.requests.sendMessage
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.wiertarbot.entities.FBMessage
import pl.kvgx12.wiertarbot.repositories.FBMessageRepository
import pl.kvgx12.wiertarbot.services.PermissionService
import pl.kvgx12.wiertarbot.services.RabbitMQService
import pl.kvgx12.wiertarbot.utils.getLogger

class FBKtEventConsumer(
    private val permissionService: PermissionService,
    private val rabbitMQService: RabbitMQService,
    private val fbMessageRepository: FBMessageRepository,
    private val fbMilestoneTracker: FBKtMilestoneTracker,
    private val transaction: TransactionTemplate,
) {
    private val log = getLogger()

    suspend fun consume(session: Session, event: Event) {
        when (event) {
            Event.Connected -> log.info("Connected")
            is Event.Disconnected -> log.warn("Disconnected $event")
            is ThreadEvent.MessageReaction -> {
                if (
                    event.author.id != session.userId
                    && permissionService.isAuthorized("doublereact", event.thread.id, event.author.id)
                ) {
                    event.message.react(session, event.reaction)
                }
            }

            is ThreadEvent.PeopleAdded -> {
                if (event.added.none { session.userId == it.id })
                    session.sendMessage(event.thread, "poziom spat")
            }

            is ThreadEvent.PersonRemoved -> {
                if (event.removed.id != session.userId)
                    session.sendMessage(event.thread, "poziom wzrus")
            }

            is ThreadEvent.UnsendMessage -> withContext(Dispatchers.IO) {
                unsendMessage(event)
            }

            is ThreadEvent.WithMessage -> withContext(Dispatchers.IO) {
                consumeMessage(session, event)
            }

            is Event.Unknown -> log.warn("Unknown event: $event")

            else -> {}
        }
    }

    private suspend fun consumeMessage(session: Session, event: ThreadEvent.WithMessage) {
        fbMilestoneTracker.events.send(session to event)

        val serialized = FBMessageSerialization.serializeMessageEvent(event)

        runCatching {
            rabbitMQService.publishMessageEvent(serialized)
        }.onFailure {
            log.error("Failed to publish message to RabbitMQ", it)
        }

        transaction.executeWithoutResult {
            fbMessageRepository.save(
                FBMessage(
                    messageId = event.message.id,
                    threadId = event.thread.id,
                    authorId = event.author.id,
                    time = event.message.createdAt ?: 0,
                    message = serialized,
                )
            )
        }
    }

    private suspend fun unsendMessage(event: ThreadEvent.UnsendMessage) {
        runCatching {
            rabbitMQService.publishMessageDelete(
                buildJsonObject {
                    put("message_id", event.message.id)
                    put("thread_id", event.thread.id)
                    put("author_id", event.author.id)
                    put("at", event.timestamp)
                }.toString()
            )
        }.onFailure {
            log.error("Failed to publish message delete to RabbitMQ", it)
        }

        transaction.executeWithoutResult {
            fbMessageRepository.markDeleted(event.message.id, event.timestamp)
        }
    }
}
