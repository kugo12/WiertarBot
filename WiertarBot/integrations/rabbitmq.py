import json
import fbchat
import aio_pika

from returns.curry import partial

from ..config import config, RabbitMQConfig

_connection: aio_pika.abc.AbstractConnection
_channel: aio_pika.abc.AbstractChannel
_exchange: aio_pika.abc.AbstractExchange

_encoding = "UTF-8"
_message = partial(aio_pika.Message, content_type="application/json", content_encoding=_encoding)

_on = config.on(RabbitMQConfig)


def message(data: str) -> aio_pika.Message:
    return _message(data.encode(_encoding))


@config.register_init
@_on
async def _init(config: RabbitMQConfig) -> None:
    global _connection, _channel, _exchange

    _connection = await aio_pika.connect(config.url)
    _channel = await _connection.channel()
    _exchange = await _channel.get_exchange(config.exchange_name)


@_on
async def publish_message_event(event: str) -> None:
    await _exchange.publish(message(event), routing_key="bot.fb.event.message.new")


@_on
async def publish_message_delete(event: fbchat.UnsendEvent) -> None:
    data = {
        "message_id": event.message.id,
        "thread_id": event.thread.id,
        "author_id": event.author.id,
        "at": event.at.timestamp()
    }

    await _exchange.publish(message(json.dumps(data)), routing_key="bot.fb.event.message.delete")


@_on
async def publish_account_locked() -> None:
    await _exchange.publish(message(""), routing_key="bot.fb.event.account.locked")
