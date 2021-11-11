import asyncio
from datetime import datetime

import fbchat

from . import perm
from .bot import WiertarBot
from .db import db
from .dispatch import EventDispatcher
from .utils import serialize_MessageEvent


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
    conn = db.get()
    cur = conn.cursor()
    mid = event.message.id
    deleted_at = int(datetime.timestamp(event.at))

    cur.execute(('SELECT mid, thread_id, author_id, time, message '
                 'FROM messages WHERE mid = ?'), [mid])
    message = cur.fetchone()
    if message:
        message += (deleted_at,)  # add deleted_at to message tuple

        cur.execute(('INSERT INTO deleted_messages '
                     '(mid, thread_id, author_id, time, message, deleted_at) '
                     'VALUES (?, ?, ?, ?, ?, ?)'), message)
        cur.execute('DELETE FROM messages WHERE mid = ?', [mid])
        conn.commit()


@EventDispatcher.slot(fbchat.MessageEvent)
async def save_message(event: fbchat.MessageEvent):
    conn = db.get()
    cur = conn.cursor()
    cur.execute(("INSERT INTO messages (mid, thread_id, author_id, time, message) "
                 "VALUES (?, ?, ?, ?, ?)"),
                [event.message.id, event.thread.id, event.author.id,
                 int(datetime.timestamp(event.message.created_at)),
                 serialize_MessageEvent(event)]
                )
    conn.commit()

    if event.message.attachments:
        await asyncio.gather(*[WiertarBot.save_attachment(i)
                               for i in event.message.attachments])
