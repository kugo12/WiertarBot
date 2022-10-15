from .. import _threads
from _typeshed import Incomplete
from typing import Any

attrs_event: Incomplete

class Event: ...

class UnknownEvent(Event):
    source: str
    data: Any

class ThreadEvent(Event):
    author: _threads.User
    thread: _threads.ThreadABC
