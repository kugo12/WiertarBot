package pl.kvgx12.wiertarbot.connector

import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.messaging.converter.ProtobufMessageConverter
import pl.kvgx12.wiertarbot.proto.MessageEvent
import pl.kvgx12.wiertarbot.proto.event

class RabbitMQ(
    private val template: RabbitTemplate,
) {
    fun publish(event: MessageEvent) = send(
        "core.event.message",
        event {
            this.message = event
        }.toByteArray(),
    )

    private fun send(
        routingKey: String,
        message: ByteArray,
        contentType: String = ProtobufMessageConverter.PROTOBUF.toString(),
    ): Unit = template.send(
        "core.event",
        routingKey,
        MessageBuilder.withBody(message).run {
            setContentType(contentType)
            build()
        },
    )
}
