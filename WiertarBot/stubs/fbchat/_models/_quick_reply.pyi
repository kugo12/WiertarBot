from .._common import attrs_default as attrs_default
from typing import Any, Optional

class QuickReply:
    payload: Any
    external_payload: Any
    data: Any
    is_response: bool

class QuickReplyText(QuickReply):
    title: Optional[str]
    image_url: Optional[str]

class QuickReplyLocation(QuickReply): ...

class QuickReplyPhoneNumber(QuickReply):
    image_url: Optional[str]

class QuickReplyEmail(QuickReply):
    image_url: Optional[str]

def graphql_to_quick_reply(q, is_response: bool = ...): ...
