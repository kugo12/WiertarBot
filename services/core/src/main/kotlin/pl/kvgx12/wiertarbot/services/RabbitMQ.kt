package pl.kvgx12.wiertarbot.services

import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.rabbit.core.RabbitTemplate
import pl.kvgx12.wiertarbot.proto.MessageEvent

class RabbitMQ(
    private val template: RabbitTemplate,
) {
    fun publish(event: MessageEvent) = send("core.event.message", event.toByteArray())

    private fun send(
        routingKey: String,
        message: ByteArray,
        contentType: String = "application/x-protobuf",
    ): Unit = template.send(
        routingKey,
        MessageBuilder.withBody(message).run {
            setContentType(contentType)
            build()
        },
    )
}
