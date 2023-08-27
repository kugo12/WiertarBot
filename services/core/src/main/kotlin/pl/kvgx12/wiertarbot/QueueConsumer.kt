package pl.kvgx12.wiertarbot

import com.google.protobuf.InvalidProtocolBufferException
import com.rabbitmq.client.Channel
import org.springframework.amqp.core.Message
import org.springframework.amqp.rabbit.annotation.RabbitListener
import pl.kvgx12.wiertarbot.proto.Event
import pl.kvgx12.wiertarbot.services.CommandService
import pl.kvgx12.wiertarbot.utils.getLogger
import pl.kvgx12.wiertarbot.utils.error

class QueueConsumer(
    private val commandService: CommandService,
) {
    private val log = getLogger()

    @RabbitListener(queues = ["core.command"])
    private suspend fun consumeMessageEvent(message: Message, channel: Channel) {
        val deliveryTag = message.messageProperties.deliveryTag
        val event = try {
            Event.parseFrom(message.body)
        } catch (e: InvalidProtocolBufferException) {
            log.error(e)
            channel.basicNack(deliveryTag, false, false)
            return
        }

        if (!event.hasMessage()) {
            log.error("Event has no message")
            channel.basicNack(deliveryTag, false, false)
            return
        }

        runCatching {
            commandService.dispatch(event.message)
        }.onFailure(log::error)

        channel.basicAck(deliveryTag, false)
    }
}
