import datetime
from . import Attachment as Attachment, Image as Image
from .._common import attrs_default as attrs_default
from _typeshed import Incomplete
from typing import Optional, Set

class FileAttachment(Attachment):
    url: Optional[str]
    size: Optional[int]
    name: Optional[str]
    is_malicious: Optional[bool]

class AudioAttachment(Attachment):
    filename: Optional[str]
    url: Optional[str]
    duration: Optional[datetime.timedelta]
    audio_type: Optional[str]

class ImageAttachment(Attachment):
    original_extension: Optional[str]
    width: Optional[int]
    height: Optional[int]
    is_animated: Optional[bool]
    previews: Set[Image]

class VideoAttachment(Attachment):
    size: Optional[int]
    width: Optional[int]
    height: Optional[int]
    duration: Optional[datetime.timedelta]
    preview_url: Optional[str]
    previews: Set[Image]

def graphql_to_attachment(data, size: Incomplete | None = ...): ...
def graphql_to_subattachment(data): ...
