import fbchat
import inspect
import importlib
from asyncio import get_running_loop, gather
from typing import Iterable, Callable, Coroutine, Any, Optional
from time import time

from . import bot, perm, config
from .response import Response

EventCallable = Callable[[fbchat.Event], Coroutine[Any, Any, None]]


class EventDispatcher:
    _slots: dict[str, list[EventCallable]] = {}

    @classmethod
    def slot(cls, event: type[fbchat.Event]):
        def wrap(func: EventCallable):
            name = event.__name__

            if name not in cls._slots:
                cls._slots[name] = []

            cls._slots[name].append(func)
            if name == 'MessageEvent':
                cls.slot(fbchat.MessageReplyEvent)(func)

            return func

        return wrap

    @classmethod
    async def send_signal(cls, event):
        name = type(event).__name__
        if name in cls._slots:
            loop = get_running_loop()
            for func in cls._slots[name]:
                loop.create_task(func(event))


class MessageEventDispatcher:
    _commands = {}
    _special = []
    _alias_of = {}
    _image_edit_queue = {}

    @staticmethod
    def register(
            *,
            name: Optional[str] = None,
            aliases: Optional[Iterable[str]] = None,
            special: bool = False
    ):
        def wrap(func):
            if special:
                MessageEventDispatcher._special.append(func)
            else:
                _name = name if name else func.__name__

                MessageEventDispatcher._commands[_name] = func
                MessageEventDispatcher._alias_of[_name] = _name

                if func.__doc__:
                    func.__doc__ = inspect.cleandoc(func.__doc__)
                    format_docstr = {
                        'prefix': config.wiertarbot.prefix,
                        'name': _name,
                        'command': config.wiertarbot.prefix + _name
                    }
                    func.__doc__ = func.__doc__.format(**format_docstr)

                if aliases:
                    if func.__doc__:
                        func.__doc__ += '\nAliasy: ' + ', '.join(aliases)

                    for alias in aliases:
                        MessageEventDispatcher._alias_of[alias] = _name

                # if permission doesn't exist in db, allow all users to use command
                if not perm.get_permission(_name):
                    perm.edit(_name, ['*'])

            return func

        return wrap

    @staticmethod
    @EventDispatcher.slot(fbchat.MessageEvent)
    async def dispatch(event: fbchat.MessageEvent):
        if event.author.id != bot.WiertarBot.session.user.id:
            if perm.check('banned', event.thread.id, event.author.id):
                pass
            else:
                if event.message.text:
                    if event.message.text.startswith(config.wiertarbot.prefix):
                        # first word without prefix
                        fw = event.message.text.split(' ', 1)[0][len(config.wiertarbot.prefix):].lower()
                        if fw in MessageEventDispatcher._alias_of:
                            fw = MessageEventDispatcher._alias_of[fw]

                            if perm.check(fw, event.thread.id, event.author.id):
                                command = MessageEventDispatcher._commands[fw]
                                try:
                                    response = await command(event)
                                    if response:
                                        await response.send()
                                except TypeError:
                                    img_edit = command()
                                    response = await img_edit.check(event)
                                    if response:
                                        # add to queue
                                        t_u_id = f'{event.thread.id}_{event.author.id}'
                                        queue = (int(time()), img_edit)
                                        MessageEventDispatcher._image_edit_queue[t_u_id] = queue

                    # run all special functions asynchronously
                    await gather(*[i(event) for i in MessageEventDispatcher._special])

                else:  # if there is no text in message
                    t_u_id = f'{event.thread.id}_{event.author.id}'
                    if t_u_id in MessageEventDispatcher._image_edit_queue:
                        t, img_edit = MessageEventDispatcher._image_edit_queue[t_u_id]

                        if t + config.image_edit_timeout > time():
                            img = await img_edit.get_image_from_attachments(event.message)
                            if img:  # if found an image
                                await img_edit.edit_and_send(event, img)
                                del MessageEventDispatcher._image_edit_queue[t_u_id]
                        else:  # if timed out
                            del MessageEventDispatcher._image_edit_queue[t_u_id]

    @classmethod
    def cleanup(cls):
        cls._special = []
        cls._alias_of = {}
        cls._commands = {}


@MessageEventDispatcher.register()
async def reload(event: fbchat.MessageEvent) -> Response:
    """
    Użycie:
        {command}
    Zwraca:
        Status
    Funkcja:
        Przeładowywuje komendy
    """

    importlib.reload(bot.WiertarBot.commands)

    MessageEventDispatcher.cleanup()
    MessageEventDispatcher.register()(reload)

    bot.WiertarBot.commands.reload()

    return Response(event, text='git')
