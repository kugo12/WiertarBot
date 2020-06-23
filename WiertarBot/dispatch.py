import fbchat
from typing import Iterable

from . import bot


class EventDispatcher():
    _slots = {}

    @staticmethod
    def slot(event):
        def wrap(func):
            name = event.__name__

            if name not in EventDispatcher._slots:
                EventDispatcher._slots[name] = []

            EventDispatcher._slots[name].append(func)

            return func
        return wrap

    @staticmethod
    async def send_signal(event):
        name = type(event).__name__
        if name in EventDispatcher._slots:
            for func in EventDispatcher._slots[name]:
                await func(event)


class Response():
    __slots__ = ['event', 'text', 'files', 'voice_clip', 'mentions', 'reply_to_id']

    def __init__(
            self,
            event: fbchat.MessageEvent,
            *,
            text: str = None,
            files: Iterable[str] = None,
            voice_clip: bool = False,
            mentions: Iterable[fbchat.Mention] = None,
            reply_to_id: str = None
            ):
        self.event = event
        self.text = text
        self.files = files
        self.voice_clip = voice_clip
        self.mentions = mentions
        self.reply_to_id = reply_to_id

    async def send(self):
        if self.files:
            self.files = await bot.WiertarBot.upload(self.files, self.voice_clip)

        mid = await self.event.thread.send_text(text=self.text, mentions=self.mentions,
                                                files=self.files, reply_to_id=self.reply_to_id)
        return mid
