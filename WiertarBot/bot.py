import fbchat
import json
import atexit
from asyncio import AbstractEventLoop

from WiertarBot import config
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
        WiertarBot.client = fbchat.Client(WiertarBot.session)
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
