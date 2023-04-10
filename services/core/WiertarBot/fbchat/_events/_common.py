import datetime

import attr
from .. import _exception, _util, _threads, _session

from typing import Any, Self


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class Event:
    """Base class for all events."""

    @staticmethod
    def _get_thread(session: _session.Session, data) -> _threads.ThreadABC:
        # TODO: Handle pages? Is it even possible?
        key = data["threadKey"]

        if "threadFbId" in key:
            return _threads.Group(session=session, id=str(key["threadFbId"]))
        elif "otherUserFbId" in key:
            return _threads.User(session=session, id=str(key["otherUserFbId"]))
        raise _exception.ParseError("Could not find thread data", data=data)


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class UnknownEvent(Event):
    """Represent an unknown event."""

    #: Some data describing the unknown event's origin
    source: str
    #: The unknown data. This cannot be relied on, it's only for debugging purposes.
    data: Any

    @classmethod
    def _parse(cls, session: _session.Session, data):
        raise NotImplementedError


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class ThreadEvent(Event):
    """Represent an event that was done by a user/page in a thread."""

    #: The person who did the action
    author: "_threads.User"  # Or Union[User, Page]?
    #: Thread that the action was done in
    thread: "_threads.ThreadABC"

    @classmethod
    def _parse_metadata(cls, session: _session.Session, data) -> tuple[_threads.User, _threads.ThreadABC, datetime.datetime]:
        metadata = data["messageMetadata"]
        author = _threads.User(session=session, id=metadata["actorFbId"])
        thread = cls._get_thread(session, metadata)
        at = _util.millis_to_datetime(int(metadata["timestamp"]))
        return author, thread, at

    @classmethod
    def _parse_fetch(cls, session: _session.Session, data) -> tuple[_threads.User, datetime.datetime]:
        author = _threads.User(session=session, id=data["message_sender"]["id"])
        at = _util.millis_to_datetime(int(data["timestamp_precise"]))
        return author, at