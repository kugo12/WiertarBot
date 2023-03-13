package pl.kvgx12.wiertarbot.connectors

import dev.inmo.micro_utils.coroutines.*
import dev.inmo.tgbotapi.abstracts.FromUser
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.requests.bot.GetMe
import dev.inmo.tgbotapi.types.files.PhotoSize
import dev.inmo.tgbotapi.types.files.TelegramMediaFile
import dev.inmo.tgbotapi.types.message.abstracts.*
import dev.inmo.tgbotapi.types.message.content.*
import dev.inmo.tgbotapi.types.update.MessageUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.TelegramAPIUrlsKeeper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import pl.kvgx12.wiertarbot.config.TelegramProperties
import pl.kvgx12.wiertarbot.connector.Connector
import pl.kvgx12.wiertarbot.events.Attachment
import pl.kvgx12.wiertarbot.events.Event
import pl.kvgx12.wiertarbot.events.ImageAttachment
import pl.kvgx12.wiertarbot.events.MessageEvent
import pl.kvgx12.wiertarbot.python.Interpreter
import pl.kvgx12.wiertarbot.utils.longPolling
import pl.kvgx12.wiertarbot.utils.text

class TelegramConnector(
    private val interpreter: Interpreter,
    telegramProperties: TelegramProperties,
) : Connector {
    val keeper = TelegramAPIUrlsKeeper(telegramProperties.token)
    val bot = telegramBot(keeper)
    val me = runBlocking { bot.execute(GetMe) }

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
        is MessageUpdate -> convert(update.data)
        else -> null
    }

    fun convert(message: Message): MessageEvent? {
        return MessageEvent(
            context = TelegramContext(this, message, interpreter),
            text = message.text ?: "",
            authorId = (message as? FromUser ?: return null).user.id.chatId.toString(),
            threadId = message.chat.id.chatId.toString(),
            at = message.date.unixMillisLong / 1000,
            mentions = emptyList(),
            externalId = message.messageId.toString(),
            replyToId = (message as? PossiblyReplyMessage)?.replyTo?.messageId?.toString(),
            attachments = getAttachments(message)
        )
    }

    fun getAttachments(message: Message) = convert((message as? ContentMessage<*>)?.content)

    fun convert(content: MessageContent?): List<Attachment> = when (content) {
        is MediaCollectionContent<*> -> content.mediaCollection.map(::convert)
        is MediaContent -> listOf(convert(content.media))
        else -> emptyList()
    }

    fun convert(file: TelegramMediaFile): Attachment = when (file) {
        is PhotoSize -> ImageAttachment(
            id = file.fileId.fileId,
            width = file.width,
            height = file.height,
            originalExtension = null,
            isAnimated = false
        )
        else -> Attachment(id=file.fileId.fileId)
    }
}