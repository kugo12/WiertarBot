package pl.kvgx12.wiertarbot

import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.runBlocking
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import pl.kvgx12.wiertarbot.proto.Event
import pl.kvgx12.wiertarbot.services.CommandService
import pl.kvgx12.wiertarbot.utils.error
import pl.kvgx12.wiertarbot.utils.getLogger

class QueueConsumer(
    private val commandService: CommandService,
) {
    private val log = getLogger()

    @RabbitListener(queues = ["core.command"])
    private fun consumeMessageEvent(message: Message) = runBlocking {
        val event = try {
            Event.parseFrom(message.body)
        } catch (e: InvalidProtocolBufferException) {
            throw AmqpRejectAndDontRequeueException("Invalid message", e)
        }

        if (!event.hasMessage()) {
            throw AmqpRejectAndDontRequeueException("Event has no message")
        }

        runCatching {
            commandService.dispatch(event.message)
        }.onFailure(log::error)

        Unit
    }
}
