from asyncio import get_running_loop
from typing import Generic, TypeVar, Union, Callable, Optional, TypeAlias

from .typing import EventCallable, EventCallableWithContext

_T_base = TypeVar("_T_base")
EventConsumer = Union[
    EventCallable[_T_base],
    EventCallableWithContext[_T_base]
]
Hook: TypeAlias = Callable[['EventDispatcher', type[_T_base], EventConsumer], None]


class EventDispatcher(Generic[_T_base]):
    def __init__(self, *, hook: Optional[Hook] = None) -> None:
        self._slots: dict[str, list[EventConsumer]] = {}
        self._hook = hook

    def on(self, event: type[_T_base]):
        def wrap(func: EventConsumer) -> EventConsumer:
            name = event.__name__

            if name not in self._slots:
                self._slots[name] = []

            self._slots[name].append(func)

            if self._hook:
                self._hook(self, event, func)

            return func

        return wrap

    async def dispatch(self, event, **kwargs) -> None:
        name = type(event).__name__
        if name in self._slots:
            loop = get_running_loop()
            for func in self._slots[name]:
                loop.create_task(func(event, **kwargs))
