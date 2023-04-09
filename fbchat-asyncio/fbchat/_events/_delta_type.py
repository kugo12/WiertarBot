import attr
import datetime
from ._common import UnknownEvent, ThreadEvent
from .. import _util, _threads, _models

from typing import Sequence, Optional


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class ColorSet(ThreadEvent):
    """Somebody set the color in a thread."""

    #: The new color. Not limited to the ones in `ThreadABC.set_color`
    color: str
    #: When the color was set
    at: datetime.datetime

    @classmethod
    def _parse(cls, session, data):
        author, thread, at = cls._parse_metadata(session, data)
        color = _threads.ThreadABC._parse_color(data["untypedData"]["theme_color"])
        return cls(author=author, thread=thread, color=color, at=at)


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class EmojiSet(ThreadEvent):
    """Somebody set the emoji in a thread."""

    #: The new emoji
    emoji: str
    #: When the emoji was set
    at: datetime.datetime

    @classmethod
    def _parse(cls, session, data):
        author, thread, at = cls._parse_metadata(session, data)
        emoji = data["untypedData"]["thread_icon"]
        return cls(author=author, thread=thread, emoji=emoji, at=at)


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class NicknameSet(ThreadEvent):
    """Somebody set the nickname of a person in a thread."""

    #: The person whose nickname was set
    subject: str
    #: The new nickname. If ``None``, the nickname was cleared
    nickname: Optional[str]
    #: When the nickname was set
    at: datetime.datetime

    @classmethod
    def _parse(cls, session, data):
        author, thread, at = cls._parse_metadata(session, data)
        subject = _threads.User(
            session=session, id=data["untypedData"]["participant_id"]
        )
        nickname = data["untypedData"]["nickname"] or None  # None if ""
        return cls(
            author=author, thread=thread, subject=subject, nickname=nickname, at=at
        )


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class AdminsAdded(ThreadEvent):
    """Somebody added admins to a group."""

    #: The people that were set as admins
    added: Sequence["_threads.User"]
    #: When the admins were added
    at: datetime.datetime

    @classmethod
    def _parse(cls, session, data):
        author, thread, at = cls._parse_metadata(session, data)
        subject = _threads.User(session=session, id=data["untypedData"]["TARGET_ID"])
        return cls(author=author, thread=thread, added=[subject], at=at)


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class AdminsRemoved(ThreadEvent):
    """Somebody removed admins from a group."""

    #: The people that were removed as admins
    removed: Sequence["_threads.User"]
    #: When the admins were removed
    at: datetime.datetime

    @classmethod
    def _parse(cls, session, data):
        author, thread, at = cls._parse_metadata(session, data)
        subject = _threads.User(session=session, id=data["untypedData"]["TARGET_ID"])
        return cls(author=author, thread=thread, removed=[subject], at=at)


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class ApprovalModeSet(ThreadEvent):
    """Somebody changed the approval mode in a group."""

    require_admin_approval: bool
    #: When the approval mode was set
    at: datetime.datetime

    @classmethod
    def _parse(cls, session, data):
        author, thread, at = cls._parse_metadata(session, data)
        raa = data["untypedData"]["APPROVAL_MODE"] == "1"
        return cls(author=author, thread=thread, require_admin_approval=raa, at=at)


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class CallStarted(ThreadEvent):
    """Somebody started a call."""

    #: When the call was started
    at: datetime.datetime

    @classmethod
    def _parse(cls, session, data):
        author, thread, at = cls._parse_metadata(session, data)
        return cls(author=author, thread=thread, at=at)


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class CallEnded(ThreadEvent):
    """Somebody ended a call."""

    #: How long the call took
    duration: datetime.timedelta
    #: When the call ended
    at: datetime.datetime

    @classmethod
    def _parse(cls, session, data):
        author, thread, at = cls._parse_metadata(session, data)
        duration = _util.seconds_to_timedelta(int(data["untypedData"]["call_duration"]))
        return cls(author=author, thread=thread, duration=duration, at=at)


@attr.s(slots=True, kw_only=True, auto_attribs=True)
class CallJoined(ThreadEvent):
    """Somebody joined a call."""

    #: When the call ended
    at: datetime.datetime

    @classmethod
    def _parse(cls, session, data):
        author, thread, at = cls._parse_metadata(session, data)
        return cls(author=author, thread=thread, at=at)


def parse_admin_message(session, data):
    type_ = data["type"]
    if type_ == "change_thread_theme":
        return ColorSet._parse(session, data)
    elif type_ == "change_thread_icon":
        return EmojiSet._parse(session, data)
    elif type_ == "change_thread_nickname":
        return NicknameSet._parse(session, data)
    elif type_ == "change_thread_admins":
        event_type = data["untypedData"]["ADMIN_EVENT"]
        if event_type == "add_admin":
            return AdminsAdded._parse(session, data)
        elif event_type == "remove_admin":
            return AdminsRemoved._parse(session, data)
        else:
            pass
    elif type_ == "change_thread_approval_mode":
        return ApprovalModeSet._parse(session, data)
    elif type_ == "instant_game_update":
        pass  # TODO: This
    elif type_ == "messenger_call_log":  # Previously "rtc_call_log"
        event_type = data["untypedData"]["event"]
        if event_type == "group_call_started":
            return CallStarted._parse(session, data)
        elif event_type in ["group_call_ended", "one_on_one_call_ended"]:
            return CallEnded._parse(session, data)
        else:
            pass
    elif type_ == "participant_joined_group_call":
        return CallJoined._parse(session, data)
    return UnknownEvent(source="Delta type", data=data)
