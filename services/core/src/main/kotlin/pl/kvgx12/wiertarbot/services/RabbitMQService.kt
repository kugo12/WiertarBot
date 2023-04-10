package pl.kvgx12.wiertarbot.services

import org.springframework.amqp.core.MessageBuilder
import org.springframework.amqp.rabbit.core.RabbitTemplate

class RabbitMQService(
    private val template: RabbitTemplate,
) {
    fun publishMessageEvent(event: String) = send("bot.fb.event.message.new", event)
    fun publishMessageDelete(event: String) = send("bot.fb.event.message.delete", event)
    fun publishAccountLocked() = send("bot.fb.event.account.locked", "", "text/plain")

    private fun send(
        routingKey: String,
        message: String,
        contentType: String = "application/json"
    ): Unit = template.send(
        routingKey,
        MessageBuilder.withBody(message.toByteArray(Charsets.UTF_8)).run {
            setContentEncoding(Charsets.UTF_8.name())
            setContentType(contentType)
            build()
        }
    )
}