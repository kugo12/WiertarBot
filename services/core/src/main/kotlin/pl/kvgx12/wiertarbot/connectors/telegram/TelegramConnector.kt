package pl.kvgx12.wiertarbot.connectors.telegram

import dev.inmo.micro_utils.coroutines.subscribe
import dev.inmo.tgbotapi.abstracts.FromUser
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.requests.bot.GetMe
import dev.inmo.tgbotapi.types.files.PhotoSize
import dev.inmo.tgbotapi.types.files.TelegramMediaFile
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.abstracts.PossiblyReplyMessage
import dev.inmo.tgbotapi.types.message.content.MediaCollectionContent
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.message.content.MessageContent
import dev.inmo.tgbotapi.types.update.MessageUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.TelegramAPIUrlsKeeper
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.kvgx12.wiertarbot.config.properties.TelegramProperties
import pl.kvgx12.wiertarbot.connector.Connector
import pl.kvgx12.wiertarbot.connectors.ContextHolder
import pl.kvgx12.wiertarbot.proto.*
import pl.kvgx12.wiertarbot.utils.longPolling
import pl.kvgx12.wiertarbot.utils.text

class TelegramConnector(
    telegramProperties: TelegramProperties,
) : Connector {
    val context = TelegramContext(this)
    val keeper = TelegramAPIUrlsKeeper(telegramProperties.token)
    val bot = telegramBot(keeper)
    val me = runBlocking { bot.execute(GetMe) }
    private val info = connectorInfo {
        connectorType = ConnectorType.TELEGRAM
        botId = me.id.chatId.toString()
    }

    init {
        ContextHolder.set(ConnectorType.TELEGRAM, context)
    }

    override fun run(): Flow<Event> = callbackFlow {
        coroutineScope {
            bot.longPolling(scope = this) {
                allUpdatesFlow.subscribe(this@coroutineScope) {
                    convert(it)?.let(::trySend)
                }
            }.join()
        }
    }

    fun convert(update: Update): Event? = when (update) {
        is MessageUpdate -> event {
            message = convert(info, update.data) ?: return null
        }

        else -> null
    }

    fun convert(info: ConnectorInfo, message: Message): MessageEvent? = messageEvent {
        connectorInfo = info
        text = message.text.orEmpty()
        authorId = (message as? FromUser ?: return null).user.id.chatId.toString()
        threadId = Json.encodeToString(message.chat.id)
        at = message.date.unixMillisLong / 1000
        externalId = message.messageId.toString()
        attachments.addAll(getAttachments(message))

        (message as? PossiblyReplyMessage)?.replyTo?.messageId?.toString()
            ?.let { replyToId = it }

        (message as? PossiblyReplyMessage)
            ?.replyTo?.let { convert(info, it) }
            ?.let { replyTo = it }
    }

    fun getAttachments(message: Message) = convert((message as? ContentMessage<*>)?.content)

    fun convert(content: MessageContent?): List<Attachment> = when (content) {
        is MediaCollectionContent<*> -> content.mediaCollection.map(::convert)
        is MediaContent -> listOf(convert(content.media))
        else -> emptyList()
    }

    fun convert(file: TelegramMediaFile) = attachment {
        id = file.fileId.fileId

        when (file) {
            is PhotoSize -> {
                image = imageAttachment {
                    width = file.width
                    height = file.height
                    isAnimated = false
                }
            }

            else -> {}
        }
    }
}
