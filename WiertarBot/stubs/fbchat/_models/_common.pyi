import datetime
import enum
from .._common import attrs_default as attrs_default
from typing import Optional

class ThreadLocation(enum.Enum):
    INBOX: str
    PENDING: str
    ARCHIVED: str
    OTHER: str

class ActiveStatus:
    active: bool
    last_active: Optional[datetime.datetime]
    in_game: Optional[bool]

class Image:
    url: str
    width: Optional[int]
    height: Optional[int]
