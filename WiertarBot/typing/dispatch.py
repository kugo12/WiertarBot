from typing import Protocol, Optional, Union, TypeVar, ClassVar, TYPE_CHECKING

import fbchat

if TYPE_CHECKING:
    from ..abc import Context
    from ..events import MessageEvent
    from ..response import Response

FBMessageEvent = Union[fbchat.MessageEvent, fbchat.MessageReplyEvent]
FBEvent = fbchat.Event

_T_event = TypeVar("_T_event", bound=FBEvent, contravariant=True)

class EventCallable(Protocol[_T_event]):
    async def __call__(self, event: _T_event, **kwargs) -> None:
        pass


class EventCallableWithContext(Protocol[_T_event]):
    async def __call__(self, event: _T_event, *, context: 'Context', **kwargs) -> None:
        pass


class MessageEventCallable(Protocol):
    __name__: ClassVar[str]

    async def __call__(self, event: 'MessageEvent') -> Optional['Response']:
        pass


EventConsumer = Union[
    EventCallable,
    EventCallableWithContext
]

MessageEventConsumer = Union[
    MessageEventCallable,
    type['ImageEditABC']
]
