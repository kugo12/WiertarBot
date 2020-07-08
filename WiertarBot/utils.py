import fbchat
import json
from datetime import datetime


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
    att_type = type(att).__name__
    out = {
        'id': att.id,
        'type': att_type
    }

    if att_type == 'ImageAttachment':
        out['original_extension'] = att.original_extension
    elif att_type == 'AudioAttachment':
        out['filename'] = att.filename
        out['audio_type'] = att.audio_type
        out['url'] = att.url
    elif att_type == 'VideoAttachment':
        out['preview_url'] = att.preview_url
    elif att_type == 'FileAttachment':
        out['url'] = att.url
        out['name'] = att.name
        out['is_malicious'] = att.is_malicious
    elif att_type == 'ShareAttachment':
        out['url'] = att.url
        out['original_url'] = att.original_url
    elif att_type in ['LocationAttachment', 'LiveLocationAttachment']:
        out['latitude'] = att.latitude
        out['longitude'] = att.longitude
        out['url'] = att.url

    return out
