import datetime
import enum
from .. import _session
from .._common import attrs_default as attrs_default
from _typeshed import Incomplete
from typing import Mapping, Optional, Sequence

class GuestStatus(enum.Enum):
    INVITED: int
    GOING: int
    DECLINED: int

ACONTEXT: Incomplete

class Plan:
    session: _session.Session
    id: str
    async def fetch(self) -> PlanData: ...
    async def edit(self, name: str, at: datetime.datetime, location_name: str = ..., location_id: str = ...): ...
    async def delete(self) -> None: ...
    async def participate(self): ...
    async def decline(self): ...

class PlanData(Plan):
    time: datetime.datetime
    title: str
    location: Optional[str]
    location_id: Optional[str]
    author_id: Optional[str]
    guests: Optional[Mapping[str, GuestStatus]]
    @property
    def going(self) -> Sequence[str]: ...
    @property
    def declined(self) -> Sequence[str]: ...
    @property
    def invited(self) -> Sequence[str]: ...
