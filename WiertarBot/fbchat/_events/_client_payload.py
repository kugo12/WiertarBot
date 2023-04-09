import attr
import datetime
from ._common import UnknownEvent, ThreadEvent
from .. import _exception, _util, _threads, _models, _session, _events

from typing import Optional, Self


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class ReactionEvent(ThreadEvent):
    """Somebody reacted to a message."""

    #: Message that the user reacted to
    message: "_models.Message"

    reaction: Optional[str]
    """The reaction.

    Not limited to the ones in `Message.react`.

    If ``None``, the reaction was removed.
    """

    @classmethod
    def _parse(cls, session: _session.Session, data) -> Self:
        thread = cls._get_thread(session, data)
        return cls(
            author=_threads.User(session=session, id=str(data["userId"])),
            thread=thread,
            message=_models.Message(thread=thread, id=data["messageId"]),
            reaction=data["reaction"] if data["action"] == 0 else None,
        )


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class UserStatusEvent(ThreadEvent):
    #: Whether the user was blocked or unblocked
    blocked: bool

    @classmethod
    def _parse(cls, session: _session.Session, data) -> Self:
        return cls(
            author=_threads.User(session=session, id=str(data["actorFbid"])),
            thread=cls._get_thread(session, data),
            blocked=not data["canViewerReply"],
        )


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class UnsendEvent(ThreadEvent):
    """Somebody unsent a message (which deletes it for everyone)."""

    #: The unsent message
    message: "_models.Message"
    #: When the message was unsent
    at: datetime.datetime

    @classmethod
    def _parse(cls, session: _session.Session, data) -> Self:
        thread = cls._get_thread(session, data)
        return cls(
            author=_threads.User(session=session, id=str(data["senderID"])),
            thread=thread,
            message=_models.Message(thread=thread, id=data["messageID"]),
            at=_util.millis_to_datetime(data["deletionTimestamp"]),
        )


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class MessageReplyEvent(ThreadEvent):
    """Somebody replied to a message."""

    #: The sent message
    message: "_models.MessageData"
    #: The message that was replied to
    replied_to: "_models.MessageData"

    @classmethod
    def _parse(cls, session: _session.Session, data) -> Self:
        metadata = data["message"]["messageMetadata"]
        thread = cls._get_thread(session, metadata)
        return cls(
            author=_threads.User(session=session, id=str(metadata["actorFbId"])),
            thread=thread,
            message=_models.MessageData._from_reply(thread, data["message"]),
            replied_to=_models.MessageData._from_reply(
                thread, data["repliedToMessage"]
            ),
        )


def parse_client_delta(session: _session.Session, data) -> _events.Event | None:
    if "deltaMessageReaction" in data:
        return ReactionEvent._parse(session, data["deltaMessageReaction"])
    elif "deltaChangeViewerStatus" in data:
        # TODO: Parse all `reason`
        if data["deltaChangeViewerStatus"]["reason"] == 2:
            return UserStatusEvent._parse(session, data["deltaChangeViewerStatus"])
    elif "liveLocationData" in data:
        return None
    elif "deltaRecallMessageData" in data:
        return UnsendEvent._parse(session, data["deltaRecallMessageData"])
    elif "deltaMessageReply" in data:
        return MessageReplyEvent._parse(session, data["deltaMessageReply"])
    return UnknownEvent(source="client payload", data=data)


def parse_client_payloads(session: _session.Session, data):
    payload = _util.parse_json("".join(chr(z) for z in data["payload"]))

    try:
        for delta in payload["deltas"]:
            yield parse_client_delta(session, delta)
    except _exception.ParseError:
        raise
    except Exception as e:
        raise _exception.ParseError("Error parsing ClientPayload", data=payload) from e