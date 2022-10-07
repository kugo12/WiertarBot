from . import Attachment as Attachment, Image as Image
from .._common import attrs_default as attrs_default
from typing import Optional

class Sticker(Attachment):
    pack: Optional[str]
    is_animated: bool
    medium_sprite_image: Optional[str]
    large_sprite_image: Optional[str]
    frames_per_row: Optional[int]
    frames_per_col: Optional[int]
    frame_count: Optional[int]
    frame_rate: Optional[int]
    image: Optional[Image]
    label: Optional[str]
