import fbchat
from requests import post, delete

from .config import stats_api
from .integrations.sentry import capture_exception


def post_message(message: str):
    try:
        post(stats_api["message_url"], data=message, headers=stats_api["headers"])
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

        delete(stats_api["message_url"], json=request, headers=stats_api["headers"])
    except Exception as e:
        capture_exception(e)
