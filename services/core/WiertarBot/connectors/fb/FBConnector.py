import atexit

import json
from asyncio import get_running_loop
from typing import Optional, NoReturn, Any, Callable

from .FBContext import FBContext
from .dispatch import FBEventDispatcher
from ... import config, fbchat
from ...services import RabbitMQService
from ...utils import execute_after_delay
from ...log import log


class FBConnector:
    __session: fbchat.Session
    __client: fbchat.Client
    __listener: fbchat.Listener
    __context: FBContext

    __context_hook: Callable[[FBContext], None]

    def __init__(self, context_hook: Callable[[FBContext], None]) -> None:
        atexit.register(self.save_cookies)
        self.__context_hook = context_hook

    async def run(self, callback) -> NoReturn:
        while True:
            try:
                await self._listen(callback)
            except fbchat.FacebookError as e:
                log.exception(e)
                if e.message in ('MQTT error: no connection', 'MQTT reconnection failed'):
                    log.info('Reconnecting mqtt...')
                    self.__listener.disconnect()
                    continue

                if 'account is locked' in e.message:
                    RabbitMQService.publishAccountLocked()

                raise e

    async def login(self) -> None:
        try:
            self.__session = await self._login()
        except (fbchat.ParseError, fbchat.NotLoggedIn) as e:
            log.exception(e)
            config.cookie_path.open('w').close()  # clear session file

            if 'account is locked' in e.message or 'Failed loading session' in e.message:
                RabbitMQService.publishAccountLocked()
                raise e

            self.__session = await self._login()

        self.__client = fbchat.Client(session=self.__session)
        self._update_context()

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
            config.wiertarbot.getEmail(), config.wiertarbot.getPassword(),
            on_2fa_callback=self._2fa_callback,
        )

    async def _listen(self, callback) -> None:
        self.__listener = fbchat.Listener(
            session=self.__session,
            chat_on=True, foreground=True
        )

        # funny sequence id fetching
        self.__client.sequence_id_callback = self.__listener.set_sequence_id
        get_running_loop().create_task(
            execute_after_delay(5, self.__client.fetch_threads(limit=1).__anext__())
        )

        async for event in self.__listener.listen():
            callback(event)
            await FBEventDispatcher.dispatch(event, context=self.__context)

    def _update_context(self) -> None:
        self.__context = FBContext(self.__client)
        self.__context_hook(self.__context)

    async def _2fa_callback(self) -> NoReturn:
        raise NotImplementedError()
