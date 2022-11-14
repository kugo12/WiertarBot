import fbchat
import inspect
from asyncio import get_running_loop, gather
from typing import Iterable, Optional, Type, TYPE_CHECKING
from time import time

from . import perm, config
from .abc import Context
from .database import PermissionRepository
from .events import MessageEvent
from .typing import MessageEventCallable, MessageEventConsumer, EventConsumer, FBEvent

if TYPE_CHECKING:
    from .commands.ABCImageEdit import ImageEditABC

class EventDispatcher:
    _slots: dict[str, list[EventConsumer]] = {}

    @classmethod
    def on(cls, event: Type[FBEvent]):
        def wrap(func: EventConsumer) -> EventConsumer:
            name = event.__name__

            if name not in cls._slots:
                cls._slots[name] = []

            cls._slots[name].append(func)
            if name == 'MessageEvent':
                cls.on(fbchat.MessageReplyEvent)(func)

            return func

        return wrap

    @classmethod
    async def dispatch(cls, event, **kwargs) -> None:
        name = type(event).__name__
        if name in cls._slots:
            loop = get_running_loop()
            for func in cls._slots[name]:
                loop.create_task(func(event, **kwargs))


class MessageEventDispatcher:
    _commands: dict[str, MessageEventConsumer] = {}
    _special: list[MessageEventCallable] = []
    _alias_of: dict[str, str] = {}
    _image_edit_queue: dict[str, tuple[int, 'ImageEditABC']] = {}

    @classmethod
    def command(cls, name: str) -> Optional[MessageEventConsumer]:
        real_name = cls._alias_of.get(name)

        return cls._commands.get(real_name) if real_name else None

    @classmethod
    def commands(cls) -> dict[str, MessageEventConsumer]:
        return cls._commands

    @classmethod
    def register(
            cls,
            *,
            name: Optional[str] = None,
            aliases: Optional[Iterable[str]] = None,
            special: bool = False
    ):
        def wrap(func: MessageEventConsumer):
            if special and not isinstance(func, type):
                cls._special.append(func)
            else:
                _name = name or func.__name__

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

    @staticmethod
    @EventDispatcher.on(fbchat.MessageEvent)
    async def dispatch(event: fbchat.MessageEvent, *, context: Context, **kwargs) -> None:
        cls = MessageEventDispatcher
        if event.author.id != context.bot_id \
                and not perm.check('banned', event.thread.id, event.author.id):
            if event.message.text:
                g_event = MessageEvent.from_fb_event(context, event)

                if g_event.text.startswith(config.wiertarbot.prefix):
                    # first word without prefix
                    fw = g_event.text.split(' ', 1)[0][len(config.wiertarbot.prefix):].lower()
                    command_name = cls._alias_of.get(fw)

                    if command_name and perm.check(command_name, g_event.thread_id, g_event.author_id):
                        command = cls._commands[command_name]

                        if isinstance(command, type):
                            img_edit = command(g_event.text)
                            if await img_edit.check(g_event):
                                # add to queue
                                t_u_id = f'{g_event.thread_id}_{g_event.author_id}'
                                queue = (int(time()), img_edit)
                                cls._image_edit_queue[t_u_id] = queue
                        else:
                            response = await command(g_event, **kwargs)
                            if response:
                                await response.send()

                # run all special functions asynchronously
                await gather(*[i(g_event, **kwargs) for i in cls._special])

            else:  # if there is no text in message
                t_u_id = f'{event.thread.id}_{event.author.id}'
                if t_u_id in cls._image_edit_queue:
                    t, img_edit = cls._image_edit_queue[t_u_id]

                    g_event = MessageEvent.from_fb_event(context, event)

                    if t + config.image_edit_timeout > time():
                        img = await img_edit.get_image_from_attachments(g_event, event.message)
                        if img:  # if found an image
                            await img_edit.edit_and_send(g_event, img)
                            del cls._image_edit_queue[t_u_id]
                    else:  # if timed out
                        del cls._image_edit_queue[t_u_id]
