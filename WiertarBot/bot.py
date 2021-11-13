import fbchat
import json
import atexit
import mimetypes
import aiohttp
import aiofiles
import asyncio
from os import path, remove
from asyncio import sleep, get_event_loop
from typing import Iterable, Optional, Sequence, Tuple
from io import BytesIO
from time import time

from . import config, unlock
from .dispatch import EventDispatcher
from .utils import execute_after_delay
from .database import FBMessage, init_db


class WiertarBot:
    _initialized: bool = False

    session: fbchat.Session = None
    client: fbchat.Client = None
    listener: fbchat.Listener = None

    def __init__(self):
        if not WiertarBot._initialized:
            from . import commands, listeners  # avoid circular import
            WiertarBot.commands = commands
            WiertarBot._initialized = True

        get_event_loop().run_until_complete(self._init())

    async def _init(self):
        try:
            WiertarBot.session = await self._login()
        except (fbchat.ParseError, fbchat.NotLoggedIn) as e:
            print(e)
            config.cookie_path.open('w').close()  # clear session file

            if 'account is locked' in e.message or 'Failed loading session' in e.message:
                config.password = unlock.FacebookUnlock()

            WiertarBot.session = await self._login()

        WiertarBot.client = fbchat.Client(session=WiertarBot.session)

        loop = get_event_loop()
        loop.create_task(self.run())

        loop.create_task(WiertarBot.message_garbage_collector())
        atexit.register(WiertarBot._save_cookies)

    @staticmethod
    def _save_cookies():
        print('Saving cookies')
        with config.cookie_path.open('w') as f:
            json.dump(WiertarBot.session.get_cookies(), f)

    @staticmethod
    def _load_cookies() -> Optional[dict]:
        try:
            with config.cookie_path.open() as f:
                return json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            return None

    async def _login(self) -> fbchat.Session:
        cookies = WiertarBot._load_cookies()
        if cookies:
            try:
                return await fbchat.Session.from_cookies(cookies)
            except fbchat.FacebookError:
                print('Error at loading session from cookies')

        return await fbchat.Session.login(config.email, config.password,
                                          on_2fa_callback=lambda: input('2fa_code: '))

    async def run(self):
        loop = get_event_loop()

        try:
            WiertarBot.listener = fbchat.Listener(session=WiertarBot.session,
                                                  chat_on=True, foreground=True)

            # funny sequence id fetching
            WiertarBot.client.sequence_id_callback = WiertarBot.listener.set_sequence_id
            loop.create_task(
                execute_after_delay(5, WiertarBot.client.fetch_threads(limit=1).__anext__())
            )

            async for event in WiertarBot.listener.listen():
                await EventDispatcher.send_signal(event)

        except fbchat.NotConnected as e:
            print(e)
            if e.message in ['MQTT error: no connection', 'MQTT reconnection failed']:
                print('Reconnecting mqtt...')
                self.listener.disconnect()
                asyncio.get_event_loop().create_task(self.run())
                return

        except (fbchat.NotLoggedIn, ValueError) as e:
            print(e)
            if 'account is locked' in e.message:
                config.password = unlock.FacebookUnlock()
                WiertarBot.session = await self._login()
                WiertarBot.client = fbchat.Client(session=WiertarBot.session)
                loop.create_task(self.run())
                return

        loop.stop()

    @staticmethod
    async def save_attachment(attachment):
        name = type(attachment).__name__
        if name in ['AudioAttachment', 'ImageAttachment', 'VideoAttachment']:
            if name == 'AudioAttachment':
                url = attachment.url
                p = str(config.attachment_save_path / attachment.filename)
            elif name == 'ImageAttachment':
                url = await WiertarBot.client.fetch_image_url(attachment.id)
                p = str(config.attachment_save_path / f'{ attachment.id }.{ attachment.original_extension }')
            elif name == 'VideoAttachment':
                url = attachment.preview_url
                p = str(config.attachment_save_path / f'{ attachment.id }.mp4')

            async with WiertarBot.session._session.get(url) as r:
                if r.status == 200:
                    async with aiofiles.open(p, mode='wb') as f:
                        await f.write(await r.read())

    @staticmethod
    async def upload(
                files: Iterable[str], voice_clip=False
            ) -> Optional[Sequence[Tuple[str, str]]]:

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

    @staticmethod
    async def message_garbage_collector():
        while True:
            t = int(time()) - config.time_to_remove_sent_messages
            messages: Iterable[FBMessage] = FBMessage\
                .select(FBMessage.message_id, FBMessage.message)\
                .where(FBMessage.time < t, FBMessage.deleted_at is None)\
                .order_by(FBMessage.time)

            for message in messages:
                deserialized_message = json.loads(message.message)

                for attachment in deserialized_message['attachments']:
                    if attachment['type'] == 'ImageAttachment':
                        p = config.attachment_save_path / f'{ attachment["id"] }.{ attachment["original_extension"] }'
                    elif attachment['type'] == 'AudioAttachment':
                        p = config.attachment_save_path / attachment['filename']
                    elif attachment['type'] == 'VideoAttachment':
                        p = config.attachment_save_path / f'{ attachment["id"] }.mp4'
                    else:
                        continue

                    if p.exists():
                        p.unlink()

                message.delete_instance()

            del messages

            await sleep(6*60*60)
