package pl.kvgx12.wiertarbot.fb.connector

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait
import pl.kvgx12.fbchat.data.events.Event
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.requests.react
import pl.kvgx12.fbchat.requests.sendMessage
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.wiertarbot.connector.utils.getLogger
import pl.kvgx12.wiertarbot.fb.entities.FBMessage
import pl.kvgx12.wiertarbot.fb.repositories.FBMessageRepository
import pl.kvgx12.wiertarbot.fb.services.PermissionService

class FBEventConsumer(
    private val permissionService: PermissionService,
    private val fbMessageRepository: FBMessageRepository,
    private val fbMilestoneTracker: FBMilestoneTracker,
    private val transaction: TransactionalOperator,
) {
    private val log = getLogger()

    suspend fun consume(session: Session, event: Event) {
        when (event) {
            Event.Connected -> log.info("Connected")
            is Event.Disconnected -> log.warn("Disconnected $event")
            is ThreadEvent.MessageReaction -> {
                if (
                    event.author.id != session.userId &&
                    permissionService.isAuthorized("doublereact", event.thread.id, event.author.id)
                ) {
                    event.message.react(session, event.reaction)
                }
            }

            is ThreadEvent.PeopleAdded -> {
                if (event.added.none { session.userId == it.id }) {
                    session.sendMessage(event.thread, "poziom spat")
                }
            }

            is ThreadEvent.PersonRemoved -> {
                if (event.removed.id != session.userId) {
                    session.sendMessage(event.thread, "poziom wzrus")
                }
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

        transaction.executeAndAwait {
            fbMessageRepository.save(
                FBMessage(
                    messageId = event.message.id,
                    threadId = event.thread.id,
                    authorId = event.author.id,
                    time = event.message.createdAt ?: 0,
                    message = FBMessageSerialization.serializeMessageEvent(event),
                ),
            )
        }
    }

    private suspend fun unsendMessage(event: ThreadEvent.UnsendMessage) {
        transaction.executeAndAwait {
            fbMessageRepository.markDeleted(event.message.id, event.timestamp)
        }
    }
}
