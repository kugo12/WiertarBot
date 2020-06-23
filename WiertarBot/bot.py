import fbchat
import json
import atexit
import mimetypes
import aiohttp
import aiofiles
from os import path, remove
from asyncio import AbstractEventLoop
from typing import Iterable

from WiertarBot import config, perm
from .dispatch import EventDispatcher


class WiertarBot():
    session: fbchat.Session = None
    client: fbchat.Client = None

    def __init__(self, loop: AbstractEventLoop):
        self.loop = loop
        loop.run_until_complete(self._init())

        atexit.register(self._save_cookies)

    async def _init(self):
        WiertarBot.session = await self._login()
        WiertarBot.client = fbchat.Client(session=WiertarBot.session)
        self.listener = fbchat.Listener(session=WiertarBot.session, chat_on=True, foreground=True)

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

        return await fbchat.Session.login(config.email, config.password)

    async def run(self):
        async for event in self.listener.listen():
            await EventDispatcher.send_signal(event)

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

    async def upload(files: Iterable[str], voice_clip=False):
        final_files = []
        cleanup = []
        for fn in files:
            mime, _ = mimetypes.guess_type(fn)
            if mime:
                if path.exists(fn):
                    f = open(fn, 'rb')
                    fn = path.basename(fn)
                    final_files.append((fn, f, mime))
                elif fn.startswith(('http://', 'https://')):
                    url = fn
                    fn = path.basename(fn)
                    download_path = path.join(config.upload_save_path, fn)

                    async with aiohttp.ClientSession() as session:
                        async with session.get(url) as r:
                            if r.status == 200:
                                f = await aiofiles.open(download_path, mode='wb')
                                await f.write(await r.read())
                                await f.close()

                                f = open(download_path, 'rb')
                                final_files.append((fn, f, mime))
                                cleanup.append(download_path)

        if final_files:
            uploaded = await WiertarBot.client.upload(final_files, voice_clip)
        else:
            return None

        for f in cleanup:
            if path.exists(f):
                remove(f)

        return uploaded