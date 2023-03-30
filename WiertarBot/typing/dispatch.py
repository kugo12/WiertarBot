from typing import Protocol, Union, TypeVar, TYPE_CHECKING

if TYPE_CHECKING:
    from ..abc import PyContext


_T_event = TypeVar("_T_event", contravariant=True)


class EventCallable(Protocol[_T_event]):
    async def __call__(self, event: _T_event, **kwargs) -> None:
        pass


class EventCallableWithContext(Protocol[_T_event]):
    async def __call__(self, event: _T_event, *, context: 'PyContext', **kwargs) -> None:
        pass


EventConsumer = Union[
    EventCallable,
    EventCallableWithContext
]
