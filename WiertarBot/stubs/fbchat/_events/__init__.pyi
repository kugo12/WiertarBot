from ._client_payload import *
from ._delta_class import *
from ._delta_type import *
from .. import _models, _threads
from ._common import Event as Event, ThreadEvent as ThreadEvent, UnknownEvent as UnknownEvent, attrs_event as attrs_event
from _typeshed import Incomplete
from collections.abc import Generator
from typing import Mapping

class Typing(ThreadEvent):
    status: bool

class FriendRequest(Event):
    author: _threads.User

class Presence(Event):
    statuses: Mapping[str, '_models.ActiveStatus']
    full: bool

class Connect(Event): ...
class Resync(Event): ...

class Disconnect(Event):
    reason: str

def parse_events(session, topic, data) -> Generator[Incomplete, None, None]: ...
