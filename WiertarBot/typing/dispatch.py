from typing import Protocol, Optional, Union, TypeVar, ClassVar, TYPE_CHECKING

if TYPE_CHECKING:
    from ..abc import Context
    from ..events import MessageEvent
    from ..response import IResponse
    from ..commands.ABCImageEdit import ImageEditABC


_T_event = TypeVar("_T_event", contravariant=True)


class EventCallable(Protocol[_T_event]):
    async def __call__(self, event: _T_event, **kwargs) -> None:
        pass


class EventCallableWithContext(Protocol[_T_event]):
    async def __call__(self, event: _T_event, *, context: 'Context', **kwargs) -> None:
        pass


class MessageEventCallable(Protocol):
    __name__: ClassVar[str]

    async def __call__(self, event: 'MessageEvent') -> Optional['IResponse']:
        pass


EventConsumer = Union[
    EventCallable,
    EventCallableWithContext
]

MessageEventConsumer = Union[
    MessageEventCallable,
    type['ImageEditABC']
]
