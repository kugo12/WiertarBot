from . import Image as Image
from .._common import attrs_default as attrs_default
from typing import Optional, Sequence

class Attachment:
    id: Optional[str]

class UnsentMessage(Attachment): ...

class ShareAttachment(Attachment):
    author: Optional[str]
    url: Optional[str]
    original_url: Optional[str]
    title: Optional[str]
    description: Optional[str]
    source: Optional[str]
    image: Optional[Image]
    original_image_url: Optional[str]
    attachments: Sequence[Attachment]
