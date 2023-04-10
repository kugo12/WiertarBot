import json

import asyncio
from datetime import datetime

from ... import fbchat
from .FBContext import FBContext
from .dispatch import FBEventDispatcher
from ...services import PermissionService, RabbitMQService
from ...utils import serialize_message_event
from ...database import FBMessage, FBMessageRepository
from ...log import log


@FBEventDispatcher.on(fbchat.Connect)
async def on_connect(event, **_) -> None:
    log.info('Connected')


@FBEventDispatcher.on(fbchat.Disconnect)
async def on_disconnect(event: fbchat.Disconnect, **_) -> None:
    log.info(f"Disconnected: {event.reason}")


@FBEventDispatcher.on(fbchat.PeopleAdded)
async def on_people_added(event: fbchat.PeopleAdded, *, context: FBContext, **_) -> None:
    if context.bot_id not in (u.id for u in event.added):
        await event.thread.send_text('poziom spat')


@FBEventDispatcher.on(fbchat.PersonRemoved)
async def on_person_removed(event: fbchat.PersonRemoved, **_) -> None:
    await event.thread.send_text('poziom wzrus')


@FBEventDispatcher.on(fbchat.ReactionEvent)
async def on_reaction(event: fbchat.ReactionEvent, *, context: FBContext, **_) -> None:
    if event.author.id != context.bot_id \
            and PermissionService.isAuthorized('doublereact', event.thread.id, event.author.id):
        await event.message.react(event.reaction)


@FBEventDispatcher.on(fbchat.UnsendEvent)
async def on_unsend(event: fbchat.UnsendEvent, **_) -> None:
    deleted_at = int(event.at.timestamp())

    serialized_event = json.dumps({
        "message_id": event.message.id,
        "thread_id": event.thread.id,
        "author_id": event.author.id,
        "at": event.at.timestamp()
    })

    try:
        RabbitMQService.publishMessageDelete(serialized_event)
    except Exception as e:
        pass

    FBMessageRepository.markDeleted(event.message.id, deleted_at)


@FBEventDispatcher.on(fbchat.MessageEvent)
async def save_message(event: fbchat.MessageEvent, *, context: FBContext, **_) -> None:
    created_at = int(datetime.timestamp(event.message.created_at))

    serialized_message = serialize_message_event(event)

    try:
        RabbitMQService.publishMessageEvent(serialized_message)
    except Exception as e:
        pass

    FBMessageRepository.saveAndFlush(
        FBMessage.new(
            message_id=event.message.id,
            thread_id=event.thread.id,
            author_id=event.author.id,
            time=created_at,
            message=serialized_message,
            deleted_at=None
        )
    )

    if event.message.attachments:
        await asyncio.gather(*[context.save_attachment(i)
                               for i in event.message.attachments])
