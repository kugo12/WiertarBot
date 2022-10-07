import fbchat
from requests import post, delete

from ..config import wiertarbot_stats
from .sentry import capture_exception

if wiertarbot_stats:
    _cfg = wiertarbot_stats

    _headers = {
        "Content-Type": "application/json",
        "API-KEY": _cfg.key
    }

    def post_message(message: str):
        try:
            post(_cfg.message_url, data=message, headers=_headers)
        except Exception as e:
            capture_exception(e)


    def delete_message(unsend: fbchat.UnsendEvent):
        try:
            request = {
                "message_id": unsend.message.id,
                "thread_id": unsend.thread.id,
                "author_id": unsend.author.id,
                "at": unsend.at.timestamp()
            }

            delete(_cfg.message_url, json=request, headers=_headers)
        except Exception as e:
            capture_exception(e)

else:
    def post_message(message: str):
        pass

    def delete_message(unsend: fbchat.UnsendEvent):
        pass
