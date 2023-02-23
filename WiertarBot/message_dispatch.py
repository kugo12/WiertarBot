import inspect
from typing import Iterable, Optional

from . import config
from .typing import MessageEventConsumer


class MessageEventDispatcher:
    _commands: dict[str, MessageEventConsumer] = {}
    _alias_of: dict[str, str] = {}

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
    ):
        def wrap(func: MessageEventConsumer):
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

            return func

        return wrap
