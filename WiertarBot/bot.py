import fbchat
import json
import mimetypes
import aiohttp
import aiofiles
from os import path
from asyncio import sleep, get_running_loop
from typing import Iterable, Optional, Sequence, Tuple
from io import BytesIO
from time import time

from . import config
from .dispatch import EventDispatcher
from .utils import execute_after_delay
from .database import FBMessageRepository
from .log import log
from .integrations.unlock import unlock_account


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

    async def login(self):
        try:
            WiertarBot.session = await self._login()
        except (fbchat.ParseError, fbchat.NotLoggedIn) as e:
            log.exception(e)
            config.cookie_path.open('w').close()  # clear session file

            if 'account is locked' in e.message or 'Failed loading session' in e.message:
                unlock_account()

            WiertarBot.session = await self._login()

        WiertarBot.client = fbchat.Client(session=WiertarBot.session)


    @staticmethod
    def save_cookies():
        log.info('Saving cookies')
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
                log.warn('Error at loading session from cookies')

        return await fbchat.Session.login(
            config.wiertarbot.email, config.wiertarbot.password,
            on_2fa_callback=None
        )


    async def _listen(self):
        WiertarBot.listener = fbchat.Listener(
            session=WiertarBot.session,
            chat_on=True, foreground=True
        )

        # funny sequence id fetching
        WiertarBot.client.sequence_id_callback = WiertarBot.listener.set_sequence_id
        get_running_loop().create_task(
            execute_after_delay(5, WiertarBot.client.fetch_threads(limit=1).__anext__())
        )

        async for event in WiertarBot.listener.listen():
            await EventDispatcher.send_signal(event)

    async def run(self):
        while True:
            try:
                await self._listen()
            except fbchat.FacebookError as e:
                log.exception(e)
                if e.message in ['MQTT error: no connection', 'MQTT reconnection failed']:
                    log.info('Reconnecting mqtt...')
                    self.listener.disconnect()
                    continue

                # if 'account is locked' in e.message:
                #     config.unlock_facebook_account()
                #     WiertarBot.session = await self._login()
                #     WiertarBot.client = fbchat.Client(session=WiertarBot.session)
                #     continue

                raise e

    @staticmethod
    async def save_attachment(attachment):
        name = type(attachment).__name__
        if name in ['AudioAttachment', 'ImageAttachment', 'VideoAttachment']:
            if name == 'AudioAttachment':
                url = attachment.url
                p = str(config.attachment_save_path / attachment.filename)
            elif name == 'ImageAttachment':
                url = await WiertarBot.client.fetch_image_url(attachment.id)
                p = str(config.attachment_save_path / f'{attachment.id}.{attachment.original_extension}')
            else:  # 'VideoAttachment'
                url = attachment.preview_url
                p = str(config.attachment_save_path / f'{attachment.id}.mp4')

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
            messages = FBMessageRepository.find_not_deleted_and_time_before(t)

            for message in messages:
                deserialized_message = json.loads(message.message)

                for attachment in deserialized_message['attachments']:
                    if attachment['type'] == 'ImageAttachment':
                        p = config.attachment_save_path / f'{attachment["id"]}.{attachment["original_extension"]}'
                    elif attachment['type'] == 'AudioAttachment':
                        p = config.attachment_save_path / attachment['filename']
                    elif attachment['type'] == 'VideoAttachment':
                        p = config.attachment_save_path / f'{attachment["id"]}.mp4'
                    else:
                        continue

                    if p.exists():
                        p.unlink()

            count = FBMessageRepository.remove_not_deleted_and_time_before(t)
            log.info(f"Deleted {count} messages from db")

            del messages

            await sleep(6 * 60 * 60)
