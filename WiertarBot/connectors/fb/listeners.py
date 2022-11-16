from .dispatch import FBEventDispatcher

import asyncio
from asyncio import get_running_loop
from datetime import datetime

import fbchat

from ... import perm
from ...abc import Context
from ...integrations.rabbitmq import publish_message_delete, publish_message_event
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
async def on_people_added(event: fbchat.PeopleAdded, *, context: Context, **_) -> None:
    if context.bot_id not in (u.id for u in event.added):
        await event.thread.send_text('poziom spat')


@FBEventDispatcher.on(fbchat.PersonRemoved)
async def on_person_removed(event: fbchat.PersonRemoved, **_) -> None:
    await event.thread.send_text('poziom wzrus')


@FBEventDispatcher.on(fbchat.ReactionEvent)
async def on_reaction(event: fbchat.ReactionEvent, *, context: Context, **_) -> None:
    if event.author.id != context.bot_id \
            and perm.check('doublereact', event.thread.id, event.author.id):
        await event.message.react(event.reaction)


@FBEventDispatcher.on(fbchat.UnsendEvent)
async def on_unsend(event: fbchat.UnsendEvent, **_) -> None:
    deleted_at = int(datetime.timestamp(event.at))

    get_running_loop() \
        .create_task(publish_message_delete(event))
    FBMessageRepository.mark_deleted(event.message.id, deleted_at)


@FBEventDispatcher.on(fbchat.MessageEvent)
async def save_message(event: fbchat.MessageEvent, *, context: Context, **_) -> None:
    created_at = int(datetime.timestamp(event.message.created_at))

    serialized_message = serialize_message_event(event)

    get_running_loop() \
        .create_task(publish_message_event(serialized_message))

    FBMessageRepository.save(
        FBMessage(
            message_id=event.message.id,
            thread_id=event.thread.id,
            author_id=event.author.id,
            time=created_at,
            message=serialized_message,
            deleted_at=None
        ),
        force_insert=True
    )

    if event.message.attachments:
        await asyncio.gather(*[context.save_attachment(i)
                               for i in event.message.attachments])

