import asyncio
from datetime import datetime

import fbchat

from . import perm, statistics
from .bot import WiertarBot
from .dispatch import EventDispatcher
from .utils import serialize_MessageEvent
from .database import FBMessage


@EventDispatcher.slot(fbchat.Connect)
async def on_connect(event: fbchat.Connect):
    print('Connected')


@EventDispatcher.slot(fbchat.PeopleAdded)
async def on_people_added(event: fbchat.PeopleAdded):
    if WiertarBot.session.user not in event.added:
        await event.thread.send_text('poziom spat')


@EventDispatcher.slot(fbchat.PersonRemoved)
async def on_person_removed(event: fbchat.PersonRemoved):
    await event.thread.send_text('poziom wzrus')


@EventDispatcher.slot(fbchat.ReactionEvent)
async def on_reaction(event: fbchat.ReactionEvent):
    if event.author.id != WiertarBot.session.user.id:
        if perm.check('doublereact', event.thread.id, event.author.id):
            await event.message.react(event.reaction)


@EventDispatcher.slot(fbchat.NicknameSet)
async def on_nickname_change(event: fbchat.NicknameSet):
    if event.author.id != WiertarBot.session.user.id:
        if perm.check('deletename', event.thread.id, event.subject.id):
            await event.thread.set_nickname(event.subject, None)
            # await self.standard_szkaluj(["!szkaluj"], {'author_id':author_id, 'thread_id':thread_id, 'thread_type':thread_type})


@EventDispatcher.slot(fbchat.UnsendEvent)
async def on_unsend(event: fbchat.UnsendEvent):
    deleted_at = int(datetime.timestamp(event.at))

    statistics.post_delete_message(event)

    FBMessage\
        .update(deleted_at=deleted_at)\
        .where(FBMessage.message_id == event.message.id)\
        .execute()


@EventDispatcher.slot(fbchat.MessageEvent)
async def save_message(event: fbchat.MessageEvent):
    created_at = int(datetime.timestamp(event.message.created_at))

    serialized_message = serialize_MessageEvent(event)
    statistics.post_message(serialized_message)

    FBMessage(
        message_id=event.message.id,
        thread_id=event.thread.id,
        author_id=event.author.id,
        time=created_at,
        message=serialized_message
    ).save()

    if event.message.attachments:
        await asyncio.gather(*[WiertarBot.save_attachment(i)
                               for i in event.message.attachments])
