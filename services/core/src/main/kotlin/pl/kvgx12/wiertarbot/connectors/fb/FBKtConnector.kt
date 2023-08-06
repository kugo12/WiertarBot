package pl.kvgx12.wiertarbot.connectors.fb

import io.ktor.util.logging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pl.kvgx12.fbchat.data.events.ThreadEvent
import pl.kvgx12.fbchat.mqtt.Listener
import pl.kvgx12.fbchat.session.Session
import pl.kvgx12.wiertarbot.config.properties.FBProperties
import pl.kvgx12.wiertarbot.connector.Connector
import pl.kvgx12.wiertarbot.proto.*
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
        val info = connectorInfo {
            connectorType = ConnectorType.FB
            botId = session.userId
        }

        coroutineScope {
            listener.await().listen()
                .collect {
                    launch {
                        runCatching {
                            eventConsumer.consume(session, it)
                        }.onFailure(log::error)
                    }

                    if (it is ThreadEvent.WithMessage) {
                        emit(
                            event {
                                message = it.toGeneric(info)
                            },
                        )
                    }
                }
        }
    }

    companion object {
        fun ThreadEvent.WithMessage.toGeneric(info: ConnectorInfo): MessageEvent = messageEvent {
            connectorInfo = info
            text = message.text.orEmpty()
            authorId = message.author.id
            threadId = message.thread.id
            at = message.createdAt ?: 0
            mentions.addAll(message.mentions.map { it.toGeneric() })
            externalId = message.id
            val rId = if (this@toGeneric is ThreadEvent.MessageReply) repliedTo.id else message.repliedTo?.id
            rId?.let { replyToId = it }
            attachments.addAll(message.attachments.map { it.toGeneric() })
        }

        fun FBAttachment.toGeneric(): Attachment = attachment {
            val att = this@toGeneric
            att.id?.let { id = it }

            when (att) {
                is FBImageAttachment -> {
                    image = imageAttachment {
                        att.width?.let { width = it }
                        att.height?.let { height = it }
                        att.originalExtension?.let { originalExtension = it }
                        att.isAnimated?.let { isAnimated = it }
                    }
                }

                else -> {}
            }
        }

        fun FBMention.toGeneric(): Mention = mention {
            threadId = user.id
            offset = this@toGeneric.offset
            length = this@toGeneric.length
        }
    }
}
