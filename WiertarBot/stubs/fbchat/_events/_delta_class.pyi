import datetime
from .. import _models, _threads
from ._common import Event as Event, ThreadEvent as ThreadEvent, UnknownEvent as UnknownEvent, attrs_event as attrs_event
from typing import Optional, Sequence

class PeopleAdded(ThreadEvent):
    thread: _threads.Group
    added: Sequence['_threads.User']
    at: datetime.datetime

class PersonRemoved(ThreadEvent):
    thread: _threads.Group
    removed: _threads.User
    at: datetime.datetime

class TitleSet(ThreadEvent):
    thread: _threads.Group
    title: Optional[str]
    at: datetime.datetime

class UnfetchedThreadEvent(Event):
    thread: _threads.ThreadABC
    message: Optional['_models.Message']

class MessagesDelivered(ThreadEvent):
    messages: Sequence['_models.Message']
    at: datetime.datetime

class ThreadsRead(Event):
    author: _threads.ThreadABC
    threads: Sequence['_threads.ThreadABC']
    at: datetime.datetime

class MessageEvent(ThreadEvent):
    message: _models.MessageData
    at: datetime.datetime

class ThreadFolder(Event):
    thread: _threads.ThreadABC
    folder: _models.ThreadLocation

def parse_delta(session, data): ...
