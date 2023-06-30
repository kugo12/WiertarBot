package pl.kvgx12.wiertarbot.connectors.fb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.kvgx12.fbchat.data.GroupData
import pl.kvgx12.fbchat.data.PageData
import pl.kvgx12.fbchat.data.UserData
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.requests.fetch
import pl.kvgx12.fbchat.requests.sendMessage
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.wiertarbot.entities.MessageCountMilestone
import pl.kvgx12.wiertarbot.repositories.MessageCountMilestoneRepository

typealias MilestoneTrackerEvent = Pair<Session, ThreadEvent.WithMessage>

class FBKtMilestoneTracker(
    private val repository: MessageCountMilestoneRepository,
) {
    private val counts = mutableMapOf<String, MessageCountMilestone>()
    private val channel = Channel<MilestoneTrackerEvent>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.Default)

    val events: SendChannel<MilestoneTrackerEvent> = channel

    init {
        channel.consumeAsFlow()
            .onEach { (session, event) ->
                update(session, event)?.let {
                    scope.launch {
                        session.sendMessage(
                            event.thread,
                            "Gratulacje, osiągnięto ~$it wiadomości",
                        )
                    }
                }
            }
            .launchIn(scope)
    }

    private suspend fun update(session: Session, event: ThreadEvent.WithMessage): Int? {
        if (event.thread.id == event.author.id) {
            return null
        }

        val milestone = counts[event.thread.id]

        return if (milestone == null) {
            val thread = session.fetch(event.thread)

            val count = when (thread) {
                null -> return null
                is GroupData -> thread.messageCount
                is PageData -> thread.messageCount
                is UserData -> thread.messageCount
            } ?: 1

            val milestone = repository.findFirstByThreadId(thread.id)
                ?: MessageCountMilestone(threadId = thread.id, count = count)

            checkThreshold(milestone.count, count).also {
                counts[thread.id] = milestone
                milestone.count = count
                withContext(Dispatchers.IO) {
                    repository.save(milestone)
                }
            }
        } else milestone.run {
            checkThreshold(count, count + 1).also {
                count += 1
                withContext(Dispatchers.IO) {
                    repository.save(this@run)
                }
            }
        }
    }

    companion object {
        const val DEFAULT_DELTA = 250_000

        private fun checkThreshold(previous: Int, current: Int, delta: Int = DEFAULT_DELTA): Int? {
            if (previous >= current) return null

            val c = current / delta
            if (previous / delta != c) {
                return c * delta
            }

            return null
        }
    }
}
