import fbchat
from requests import post, delete

from ..config import config, WiertarBotStatsConfig
from .sentry import capture_exception

_on = config.on(WiertarBotStatsConfig)


@_on
def post_message(config: WiertarBotStatsConfig, message: str) -> None:
    try:
        post(config.message_url, data=message, headers=config.headers)
    except Exception as e:
        capture_exception(e)


@_on
def delete_message(config: WiertarBotStatsConfig, unsend: fbchat.UnsendEvent) -> None:
    try:
        request = {
            "message_id": unsend.message.id,
            "thread_id": unsend.thread.id,
            "author_id": unsend.author.id,
            "at": unsend.at.timestamp()
        }

        delete(config.message_url, json=request, headers=config.headers)
    except Exception as e:
        capture_exception(e)
