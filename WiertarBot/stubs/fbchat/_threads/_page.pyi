import datetime
from .. import _models, _session
from .._common import attrs_default as attrs_default
from ._abc import ThreadABC as ThreadABC
from typing import Optional

class Page(ThreadABC):
    session: _session.Session
    id: str

class PageData(Page):
    photo: _models.Image
    name: str
    last_active: Optional[datetime.datetime]
    message_count: Optional[int]
    plan: Optional[_models.PlanData]
    url: Optional[str]
    city: Optional[str]
    likes: Optional[int]
    sub_title: Optional[str]
    category: Optional[str]
