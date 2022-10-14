import asyncio
from datetime import datetime

import fbchat

from . import perm
from .context import Context
from .integrations.rabbitmq import publish_message_delete, publish_message_event
from .dispatch import EventDispatcher
from .utils import serialize_message_event
from .database import FBMessage, FBMessageRepository
from .log import log


@EventDispatcher.on(fbchat.Connect)
async def on_connect(event, **kwargs) -> None:
    log.info('Connected')


@EventDispatcher.on(fbchat.PeopleAdded)
async def on_people_added(event: fbchat.PeopleAdded, *, context: Context, **kwargs) -> None:
    if context.bot_id not in (u.id for u in event.added):
        await event.thread.send_text('poziom spat')


@EventDispatcher.on(fbchat.PersonRemoved)
async def on_person_removed(event: fbchat.PersonRemoved, **kwargs) -> None:
    await event.thread.send_text('poziom wzrus')


@EventDispatcher.on(fbchat.ReactionEvent)
async def on_reaction(event: fbchat.ReactionEvent, *, context: Context, **kwargs) -> None:
    if event.author.id != context.bot_id \
            and perm.check('doublereact', event.thread.id, event.author.id):
        await event.message.react(event.reaction)


@EventDispatcher.on(fbchat.UnsendEvent)
async def on_unsend(event: fbchat.UnsendEvent, **kwargs) -> None:
    deleted_at = int(datetime.timestamp(event.at))

    await publish_message_delete(event)
    FBMessageRepository.mark_deleted(event.message.id, deleted_at)


@EventDispatcher.on(fbchat.MessageEvent)
async def save_message(event: fbchat.MessageEvent, *, context: Context, **kwargs) -> None:
    created_at = int(datetime.timestamp(event.message.created_at))

    serialized_message = serialize_message_event(event)

    await publish_message_event(serialized_message)

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
