import fbchat
import json
import asyncio
from typing import Awaitable, Any
from datetime import datetime


async def execute_after_delay(delay: int, async_function: Awaitable):
    await asyncio.sleep(delay)
    await async_function


def serialize_MessageEvent(event: fbchat.MessageEvent) -> str:
    out = {
        'thread_id': event.thread.id,
        'message_id': event.message.id,
        'author_id': event.author.id,
        'at': datetime.timestamp(event.message.created_at),
        'text': event.message.text,
        'reply_to_id': event.message.reply_to_id,
        'forwarded': event.message.forwarded,
        # fbchat object types
        # 'quick_replies': [],
        'mentions': [{'thread_id': i.thread_id, 'offset': i.offset, 'length': i.length}
                     for i in event.message.mentions  # list comprehension
                     ] if event.message.mentions else [],
        'attachments': [attachment_to_dict(i)
                        for i in event.message.attachments  # list comprehension
                        ] if event.message.attachments else [],
    }

    sticker = event.message.sticker
    out['sticker_id'] = sticker.id if sticker else None

    return json.dumps(out)


def attachment_to_dict(att: fbchat.Attachment) -> dict:
    out: dict[str, Any] = {
        'id': att.id,
        'type': type(att).__name__
    }

    if isinstance(att, fbchat.ImageAttachment):
        out['original_extension'] = att.original_extension
    elif isinstance(att, fbchat.AudioAttachment):
        out['filename'] = att.filename
        out['audio_type'] = att.audio_type
        out['url'] = att.url
    elif isinstance(att, fbchat.VideoAttachment):
        out['preview_url'] = att.preview_url
    elif isinstance(att, fbchat.FileAttachment):
        out['url'] = att.url
        out['name'] = att.name
        out['is_malicious'] = att.is_malicious
    elif isinstance(att, fbchat.ShareAttachment):
        out['url'] = att.url
        out['original_url'] = att.original_url
    elif isinstance(att, fbchat.LocationAttachment) or isinstance(att, fbchat.LiveLocationAttachment):
        out['latitude'] = att.latitude
        out['longitude'] = att.longitude
        out['url'] = att.url

    return out
