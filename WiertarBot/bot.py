import fbchat
import json
import atexit
import mimetypes
import aiohttp
import aiofiles
import asyncio
from datetime import datetime
from os import path
from asyncio import AbstractEventLoop
from typing import Iterable
from io import BytesIO

from . import config, perm
from .db import db
from .dispatch import EventDispatcher
from .utils import serialize_MessageEvent


class WiertarBot():
    session: fbchat.Session = None
    client: fbchat.Client = None
    loop: AbstractEventLoop = None

    def __init__(self, loop: AbstractEventLoop):
        from . import commands  # avoid circular import
        WiertarBot.commands = commands

        WiertarBot.loop = loop
        loop.run_until_complete(self._init())

    async def _init(self):
        WiertarBot.session = await self._login()
        WiertarBot.client = fbchat.Client(session=WiertarBot.session)
        self.listener = fbchat.Listener(session=WiertarBot.session, chat_on=True, foreground=True)

        atexit.register(self._save_cookies)

    def _save_cookies(self):
        print('Saving cookies')
        with open(config.cookie_path, 'w') as f:
            json.dump(WiertarBot.session.get_cookies(), f)

    def _load_cookies(self):
        try:
            with open(config.cookie_path) as f:
                return json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            return None

    async def _login(self):
        cookies = self._load_cookies()
        if cookies:
            try:
                return await fbchat.Session.from_cookies(cookies)
            except fbchat.FacebookError:
                print('Error at loading session from cookies')

        return await fbchat.Session.login(config.email, config.password,
                                          on_2fa_callback=lambda: input('2fa_code: '))

    async def run(self):
        async for event in self.listener.listen():
            await EventDispatcher.send_signal(event)

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
            if perm.check('doublereact', event.author.id, event.thread.id):
                await event.message.react(event.reaction)

    @EventDispatcher.slot(fbchat.NicknameSet)
    async def on_nickname_change(event: fbchat.NicknameSet):
        if event.author.id != WiertarBot.session.user.id:
            if perm.check('deletename', event.subject.id, event.thread.id):
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
                     int(datetime.timestamp(event.at)), serialize_MessageEvent(event)]
                    )
        conn.commit()

        if event.message.attachments:
            await asyncio.gather(*[WiertarBot.save_attachment(i)
                                   for i in event.message.attachments])

    async def save_attachment(attachment):
        name = type(attachment).__name__
        if name in ['AudioAttachment', 'ImageAttachment', 'VideoAttachment']:
            if name == 'AudioAttachment':
                url = attachment.url
                p = path.join(config.attachment_save_path,
                              attachment.filename)
            elif name == 'ImageAttachment':
                url = await WiertarBot.client.fetch_image_url(attachment.id)
                p = path.join(config.attachment_save_path,
                              f'{ attachment.id }.{ attachment.original_extension }')
            elif name == 'VideoAttachment':
                url = attachment.preview_url
                p = path.join(config.attachment_save_path,
                              f'{ attachment.id }.mp4')

            async with WiertarBot.session._session.get(url) as r:
                if r.status == 200:
                    f = await aiofiles.open(p, mode='wb')
                    await f.write(await r.read())
                    await f.close()

    async def upload(files: Iterable[str], voice_clip=False):
        final_files = []
        for fn in files:
            if fn.startswith(('http://', 'https://')):
                true_fn = path.basename(fn.split('?', 1)[0])  # without get params
                mime, _ = mimetypes.guess_type(true_fn)
            else:
                mime, _ = mimetypes.guess_type(fn)

            if mime:
                if path.exists(fn):
                    f = open(fn, 'rb')
                    true_fn = path.basename(fn)

                    if mime == 'video/mp4' and voice_clip:
                        mime = 'audio/mp4'

                    final_files.append((true_fn, f, mime))

                elif fn.startswith(('http://', 'https://')):

                    async with aiohttp.ClientSession() as session:
                        async with session.get(fn) as r:
                            if r.status == 200:
                                f = BytesIO(await r.read())
                                final_files.append((true_fn, f, mime))

        if final_files:
            uploaded = await WiertarBot.client.upload(final_files, voice_clip)
        else:
            return None

        return uploaded
