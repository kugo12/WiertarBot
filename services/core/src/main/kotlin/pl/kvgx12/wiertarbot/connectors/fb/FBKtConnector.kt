package pl.kvgx12.wiertarbot.connectors.fb

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.mqtt.Listener
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.wiertarbot.config.FBProperties
import pl.kvgx12.wiertarbot.connector.Connector
import pl.kvgx12.wiertarbot.events.*
import pl.kvgx12.wiertarbot.utils.getLogger
import pl.kvgx12.fbchat.data.Attachment as FBAttachment
import pl.kvgx12.fbchat.data.ImageAttachment as FBImageAttachment
import pl.kvgx12.fbchat.data.Mention as FBMention

class FBKtConnector(
    private val props: FBProperties,
    private val eventConsumer: FBKtEventConsumer,
) : Connector {
    val log = getLogger()

    private val scope = CoroutineScope(Dispatchers.Default)
    val session = scope.async {
        Session(props.email, props.password)
    }

    val listener = scope.async {
        Listener(session.await())
    }

    override fun run(): Flow<Event> = flow {
        val session = session.await()
        val context = FBKtContext(session)

        coroutineScope {
            listener.await().listen()
                .collect {
                    launch { eventConsumer.consume(session, it) }

                    if (it is ThreadEvent.WithMessage)
                        emit(it.toGeneric(context))
                }
        }
    }

    companion object {
        fun ThreadEvent.WithMessage.toGeneric(context: FBKtContext): MessageEvent = MessageEvent(
            context,
            text = message.text ?: "",
            authorId = message.author.id,
            threadId = message.thread.id,
            at = message.createdAt ?: 0,
            mentions = message.mentions.map { it.toGeneric() },
            externalId = message.id,
            replyToId = if (this is ThreadEvent.MessageReply) repliedTo.id else message.repliedTo?.id,
            attachments = message.attachments.map { it.toGeneric() },
        )

        fun FBAttachment.toGeneric(): Attachment = when (this) {
            is FBImageAttachment -> ImageAttachment(
                id = id, width = width, height = height, originalExtension = originalExtension, isAnimated = isAnimated
            )

            else -> Attachment(id)
        }

        fun FBMention.toGeneric(): Mention = Mention(threadId = user.id, offset = offset, length = length)
    }
}
