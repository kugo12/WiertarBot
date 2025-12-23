package pl.kvgx12.wiertarbot.telegram

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import pl.kvgx12.telegram.TelegramClient
import pl.kvgx12.telegram.TelegramWebhook
import pl.kvgx12.telegram.data.TMessage
import pl.kvgx12.telegram.data.Update
import pl.kvgx12.wiertarbot.connector.Connector
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.proto.ConnectorType.TELEGRAM

class TelegramKtConnector(
    props: TelegramProperties,
    private val webhook: TelegramWebhook?,
) : Connector {
    val client = TelegramClient(props.token)
    val me = runBlocking { client.getMe() }

    val info = connectorInfo {
        connectorType = TELEGRAM
        botId = me.id.toString()
    }

    override fun run(): Flow<Event> = flow {
        if (webhook != null) {
            webhook.initWebhook()

            try {
                emitAll(webhook.updates())
            } finally {
                webhook.close()
            }
        } else {
            emitAll(client.getUpdates())
        }
    }.mapNotNull(::toEvent)

    private fun toEvent(u: Update): Event? = when (u) {
        is Update.Message -> toMessage(u.data)?.let { msgEvent ->
            event {
                message = msgEvent
            }
        }

        // TODO
        is Update.EditedMessage -> null
        is Update.ChatMember -> null
        is Update.MyChatMember -> null
    }

    private fun toMessage(m: TMessage): MessageEvent? = messageEvent {
        connectorInfo = info

        text = m.caption ?: m.text ?: ""
        threadId = m.chat.id.toString()
        authorId = m.from?.id?.toString()
            ?: m.viaBot?.id?.toString()
                ?: return null
        at = m.date
        messageId = m.messageId.toString()
        attachments.addAll(getAttachments(m))

        m.replyToMessage
            ?.let(::toMessage)
            ?.let {
                replyToId = it.messageId
                replyTo = it
            }
    }

    private fun getAttachments(message: TMessage): List<Attachment> {
        val attachments = mutableListOf<Attachment>()

        message.photo.forEach { photo ->
            attachments += attachment {
                id = photo.fileId
                image = imageAttachment {
                    width = photo.width
                    height = photo.height
                    isAnimated = false
                    originalExtension = "jpg"
                }
            }
        }

        return attachments
    }
}
