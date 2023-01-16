import inspect
from asyncio import get_running_loop
from typing import Iterable, Optional, TYPE_CHECKING
from time import time

from . import perm, config
from .database import PermissionRepository
from .events import MessageEvent
from .typing import MessageEventCallable, MessageEventConsumer

if TYPE_CHECKING:
    from .commands.ABCImageEdit import ImageEditABC


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

    @classmethod
    async def dispatch(cls, event: MessageEvent, **kwargs) -> None:
        if not perm.check('banned', event.thread_id, event.author_id):
            if event.text:
                if event.text.startswith(config.wiertarbot.prefix):
                    # first word without prefix
                    fw = event.text.split(' ', 1)[0][len(config.wiertarbot.prefix):].lower()
                    command_name = cls._alias_of.get(fw)

                    if command_name and perm.check(command_name, event.thread_id, event.author_id):
                        command = cls._commands[command_name]

                        if isinstance(command, type):
                            img_edit = command(event.text)
                            if await img_edit.check(event):
                                # add to queue
                                t_u_id = f'{event.thread_id}_{event.author_id}'
                                queue = (int(time()), img_edit)
                                cls._image_edit_queue[t_u_id] = queue
                        else:
                            response = await command(event, **kwargs)
                            if response:
                                await response.send()

                get_running_loop().create_task(cls._run_special(event, **kwargs))

            else:  # if there is no text in message
                t_u_id = f'{event.thread_id}_{event.author_id}'
                if t_u_id in cls._image_edit_queue:
                    t, img_edit = cls._image_edit_queue[t_u_id]

                    if t + config.image_edit_timeout > time():
                        img = await img_edit.get_image_from_attachments(event, event.attachments)
                        if img:  # if found an image
                            await img_edit.edit_and_send(event, img)
                            del cls._image_edit_queue[t_u_id]
                    else:  # if timed out
                        del cls._image_edit_queue[t_u_id]

    @classmethod
    async def _run_special(cls, event: MessageEvent, **kwargs) -> None:
        for it in cls._special:
            await it(event, **kwargs)
