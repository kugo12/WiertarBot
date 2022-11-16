import atexit

import fbchat
import json
from asyncio import sleep, get_running_loop
from typing import Optional, NoReturn, Any
from time import time

from .FBContext import FBContext
from .dispatch import FBEventDispatcher
from ... import config
from ...abc import Connector
from ...utils import execute_after_delay
from ...database import FBMessageRepository
from ...log import log
from ...integrations.rabbitmq import publish_account_locked


class FBConnector(Connector):
    __session: fbchat.Session
    __client: fbchat.Client
    __listener: fbchat.Listener

    @classmethod
    async def create(cls) -> Connector:
        self = cls()
        await self.login()

        atexit.register(self.save_cookies)
        get_running_loop().create_task(
            self.message_garbage_collector()
        )

        return self

    async def run(self) -> NoReturn:
        while True:
            try:
                await self._listen()
            except fbchat.FacebookError as e:
                log.exception(e)
                if e.message in ('MQTT error: no connection', 'MQTT reconnection failed'):
                    log.info('Reconnecting mqtt...')
                    self.__listener.disconnect()
                    continue

                if 'account is locked' in e.message:
                    await publish_account_locked()
                    self.__session = await self._login()
                    self.__client = fbchat.Client(session=self.__session)
                    continue

                raise e

    async def login(self) -> None:
        try:
            self.__session = await self._login()
        except (fbchat.ParseError, fbchat.NotLoggedIn) as e:
            log.exception(e)
            config.cookie_path.open('w').close()  # clear session file

            if 'account is locked' in e.message or 'Failed loading session' in e.message:
                await publish_account_locked()

            self.__session = await self._login()

        self.__client = fbchat.Client(session=self.__session)

    def save_cookies(self) -> None:
        log.info('Saving cookies')
        with config.cookie_path.open('w') as f:
            json.dump(self.__session.get_cookies(), f)

    @staticmethod
    def _load_cookies() -> Optional[Any]:
        try:
            with config.cookie_path.open() as f:
                return json.load(f)
        except (FileNotFoundError, json.JSONDecodeError):
            return None

    async def _login(self) -> fbchat.Session:
        cookies = self._load_cookies()
        if cookies:
            try:
                return await fbchat.Session.from_cookies(cookies)
            except fbchat.FacebookError:
                log.warn('Error at loading session from cookies')

        return await fbchat.Session.login(
            config.wiertarbot.email, config.wiertarbot.password,
            on_2fa_callback=self._2fa_callback
        )

    async def _listen(self) -> None:
        self.__listener = fbchat.Listener(
            session=self.__session,
            chat_on=True, foreground=True
        )

        context = FBContext(self.__client)

        # funny sequence id fetching
        self.__client.sequence_id_callback = self.__listener.set_sequence_id
        get_running_loop().create_task(
            execute_after_delay(5, self.__client.fetch_threads(limit=1).__anext__())
        )

        async for event in self.__listener.listen():
            await FBEventDispatcher.dispatch(event, context=context)

    async def _2fa_callback(self) -> NoReturn:
        raise NotImplementedError()

    @staticmethod
    async def message_garbage_collector() -> NoReturn:
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
