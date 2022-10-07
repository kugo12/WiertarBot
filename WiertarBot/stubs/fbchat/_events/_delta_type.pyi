import datetime
from .. import _models, _threads
from ._common import ThreadEvent as ThreadEvent, UnknownEvent as UnknownEvent, attrs_event as attrs_event
from typing import Optional, Sequence

class ColorSet(ThreadEvent):
    color: str
    at: datetime.datetime

class EmojiSet(ThreadEvent):
    emoji: str
    at: datetime.datetime

class NicknameSet(ThreadEvent):
    subject: str
    nickname: Optional[str]
    at: datetime.datetime

class AdminsAdded(ThreadEvent):
    added: Sequence['_threads.User']
    at: datetime.datetime

class AdminsRemoved(ThreadEvent):
    removed: Sequence['_threads.User']
    at: datetime.datetime

class ApprovalModeSet(ThreadEvent):
    require_admin_approval: bool
    at: datetime.datetime

class CallStarted(ThreadEvent):
    at: datetime.datetime

class CallEnded(ThreadEvent):
    duration: datetime.timedelta
    at: datetime.datetime

class CallJoined(ThreadEvent):
    at: datetime.datetime

class PollCreated(ThreadEvent):
    poll: _models.Poll
    at: datetime.datetime

class PollVoted(ThreadEvent):
    poll: _models.Poll
    added_ids: Sequence[str]
    removed_ids: Sequence[str]
    at: datetime.datetime

class PlanCreated(ThreadEvent):
    plan: _models.PlanData
    at: datetime.datetime

class PlanEnded(ThreadEvent):
    plan: _models.PlanData
    at: datetime.datetime

class PlanEdited(ThreadEvent):
    plan: _models.PlanData
    at: datetime.datetime

class PlanDeleted(ThreadEvent):
    plan: _models.PlanData
    at: datetime.datetime

class PlanResponded(ThreadEvent):
    plan: _models.PlanData
    take_part: bool
    at: datetime.datetime

def parse_admin_message(session, data): ...
