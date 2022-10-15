import datetime
from .. import _models
from ._common import ThreadEvent as ThreadEvent, UnknownEvent as UnknownEvent, attrs_event as attrs_event
from _typeshed import Incomplete
from collections.abc import Generator
from typing import Optional

class ReactionEvent(ThreadEvent):
    message: _models.Message
    reaction: Optional[str]

class UserStatusEvent(ThreadEvent):
    blocked: bool

class LiveLocationEvent(ThreadEvent): ...

class UnsendEvent(ThreadEvent):
    message: _models.Message
    at: datetime.datetime

class MessageReplyEvent(ThreadEvent):
    message: _models.MessageData
    replied_to: _models.MessageData

def parse_client_delta(session, data): ...
def parse_client_payloads(session, data) -> Generator[Incomplete, None, None]: ...
