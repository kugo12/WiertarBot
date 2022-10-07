import datetime
from . import Attachment as Attachment, Image as Image
from .._common import attrs_default as attrs_default
from typing import Optional

class LocationAttachment(Attachment):
    latitude: Optional[float]
    longitude: Optional[float]
    image: Optional[Image]
    url: Optional[str]
    address: Optional[str]

class LiveLocationAttachment(LocationAttachment):
    name: Optional[str]
    expires_at: Optional[datetime.datetime]
    is_expired: Optional[bool]
