import fbchat
import asyncio
import inspect
from typing import Iterable

from . import bot, perm, config


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
            # for func in EventDispatcher._slots[name]:
            #     await func(event)
            await asyncio.gather(*[i(event) for i in EventDispatcher._slots[name]])


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


class MessageEventDispatcher():
    _commands = {}
    _special = []
    _alias_of = {}

    def register(
            *,
            name: str = None,
            aliases: Iterable[str] = None,
            special: bool = False
            ):
        def wrap(func):
            if special:
                MessageEventDispatcher._special.append(func)
            else:
                _name = name if name else func.__name__

                MessageEventDispatcher._commands[_name] = func
                MessageEventDispatcher._alias_of[_name] = _name

                func.__doc__ = inspect.cleandoc(func.__doc__)
                format_docstr = {
                    'prefix': config.prefix,
                    'name': _name,
                    'command': config.prefix+_name
                }
                func.__doc__ = func.__doc__.format(**format_docstr)

                if aliases:
                    func.__doc__ += '\nAliasy: ' + ', '.join(aliases)

                    for alias in aliases:
                        MessageEventDispatcher._alias_of[alias] = _name

            return func
        return wrap

    @EventDispatcher.slot(fbchat.MessageEvent)
    async def dispatch(event: fbchat.MessageEvent):
        if event.author.id != bot.WiertarBot.session.user.id:
            if perm.check('banned', event.thread.id, event.author.id):
                pass
            else:
                if event.message.text:
                    if event.message.text.startswith(config.prefix):
                        # first word without prefix
                        fw = event.message.text.split(' ', 1)[0][len(config.prefix):]
                        if fw in MessageEventDispatcher._alias_of:
                            fw = MessageEventDispatcher._alias_of[fw]

                            if perm.check(fw, event.thread.id, event.author.id):
                                response = await MessageEventDispatcher._commands[fw](event)
                                if response:
                                    await response.send()

                    # run all special functions asynchronously
                    await asyncio.gather(*[i(event) for i in MessageEventDispatcher._special])
