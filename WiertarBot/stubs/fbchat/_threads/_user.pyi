import datetime
from .. import _models, _session
from .._common import attrs_default as attrs_default, log as log
from ._abc import ThreadABC as ThreadABC
from _typeshed import Incomplete
from typing import Optional

GENDERS: Incomplete

class User(ThreadABC):
    session: _session.Session
    id: str
    def __init__(self, session: _session.Session, id: str): ...
    async def confirm_friend_request(self) -> None: ...
    async def remove_friend(self) -> None: ...
    async def block(self) -> None: ...
    async def unblock(self) -> None: ...

class UserData(User):
    photo: _models.Image
    name: str
    is_friend: bool
    first_name: str
    last_name: Optional[str]
    last_active: Optional[datetime.datetime]
    message_count: Optional[int]
    plan: Optional[_models.PlanData]
    url: Optional[str]
    gender: Optional[str]
    affinity: Optional[float]
    nickname: Optional[str]
    own_nickname: Optional[str]
    color: Optional[str]
    emoji: Optional[str]
