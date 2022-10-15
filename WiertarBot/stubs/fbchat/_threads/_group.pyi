import abc
import datetime
from .. import _models, _session
from .._common import attrs_default as attrs_default
from ._abc import ThreadABC as ThreadABC
from typing import Iterable, Mapping, Optional, Set

class Group(ThreadABC):
    session: _session.Session
    id: str
    def __init__(self, *, session: _session.Session, id: str): ...
    async def add_participants(self, user_ids: Iterable[str]): ...
    async def remove_participant(self, user_id: str): ...
    async def add_admins(self, user_ids: Iterable[str]): ...
    async def remove_admins(self, user_ids: Iterable[str]): ...
    async def set_title(self, title: str): ...
    async def set_image(self, image_id: str): ...
    async def set_approval_mode(self, require_admin_approval: bool): ...
    async def accept_users(self, user_ids: Iterable[str]): ...
    async def deny_users(self, user_ids: Iterable[str]): ...

class GroupData(Group):
    photo: Optional[_models.Image]
    name: Optional[str]
    last_active: Optional[datetime.datetime]
    message_count: Optional[int]
    plan: Optional[_models.PlanData]
    participants: Set[ThreadABC]
    nicknames: Mapping[str, str]
    color: Optional[str]
    emoji: Optional[str]
    admins: Set[str]
    approval_mode: Optional[bool]
    approval_requests: Set[str]
    join_link: Optional[str]

class NewGroup(ThreadABC, metaclass=abc.ABCMeta):
    session: _session.Session
    @property
    def id(self) -> None: ...  # type: ignore
