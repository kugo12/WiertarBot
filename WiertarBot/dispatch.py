import fbchat
import inspect
from asyncio import get_running_loop, gather
from typing import Iterable, Callable, Coroutine, Any, Optional
from time import time

from . import bot, perm, config
from .commands.ABCImageEdit import ImageEditABC
from .database import PermissionRepository
from .response import Response

EventCallable = Callable[[fbchat.Event], Coroutine[Any, Any, None]]


class EventDispatcher:
    _slots: dict[str, list[EventCallable]] = {}

    @classmethod
    def on(cls, event: type[fbchat.Event]):
        def wrap(func: EventCallable):
            name = event.__name__

            if name not in cls._slots:
                cls._slots[name] = []

            cls._slots[name].append(func)
            if name == 'MessageEvent':
                cls.on(fbchat.MessageReplyEvent)(func)

            return func

        return wrap

    @classmethod
    async def dispatch(cls, event):
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

    @classmethod
    def register(
            cls,
            *,
            name: Optional[str] = None,
            aliases: Optional[Iterable[str]] = None,
            special: bool = False
    ):
        def wrap(func):
            if special:
                cls._special.append(func)
            else:
                _name = name if name else func.__name__

                cls._commands[_name] = func
                cls._alias_of[_name] = _name

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
                        cls._alias_of[alias] = _name

                # if permission doesn't exist in db, allow all users to use command
                if not PermissionRepository.find_by_command(_name):
                    perm.edit(_name, ['*'])

            return func

        return wrap

    @classmethod
    @EventDispatcher.on(fbchat.MessageEvent)
    async def dispatch(cls, event: fbchat.MessageEvent):
        if event.author.id != bot.WiertarBot.session.user.id \
                and not perm.check('banned', event.thread.id, event.author.id):
            if event.message.text:
                if event.message.text.startswith(config.wiertarbot.prefix):
                    # first word without prefix
                    fw = event.message.text.split(' ', 1)[0][len(config.wiertarbot.prefix):].lower()
                    fw = cls._alias_of.get(fw)

                    if fw and perm.check(fw, event.thread.id, event.author.id):
                        command = cls._commands[fw]

                        if isinstance(command, type):
                            if issubclass(command, ImageEditABC):
                                img_edit = command()
                                response = await img_edit.check(event)
                                if response:
                                    # add to queue
                                    t_u_id = f'{event.thread.id}_{event.author.id}'
                                    queue = (int(time()), img_edit)
                                    cls._image_edit_queue[t_u_id] = queue
                        else:
                            response = await command(event)
                            if response:
                                await response.send()

                # run all special functions asynchronously
                await gather(*[i(event) for i in cls._special])

            else:  # if there is no text in message
                t_u_id = f'{event.thread.id}_{event.author.id}'
                if t_u_id in cls._image_edit_queue:
                    t, img_edit = cls._image_edit_queue[t_u_id]

                    if t + config.image_edit_timeout > time():
                        img = await img_edit.get_image_from_attachments(event.message)
                        if img:  # if found an image
                            await img_edit.edit_and_send(event, img)
                            del cls._image_edit_queue[t_u_id]
                    else:  # if timed out
                        del cls._image_edit_queue[t_u_id]
