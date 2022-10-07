import datetime
import enum
from . import _attachment, _quick_reply, _sticker
from .. import _threads
from .._common import attrs_default as attrs_default, log as log
from typing import Any, Mapping, Optional, Sequence

class EmojiSize(enum.Enum):
    LARGE: str
    MEDIUM: str
    SMALL: str

class Mention:
    thread_id: str
    offset: int
    length: int

class Message:
    thread: _threads.ThreadABC
    id: str
    def __init__(self, thread: _threads.ThreadABC, id: str): ...
    @property
    def session(self): ...
    async def delete(self) -> None: ...
    async def unsend(self) -> None: ...
    async def react(self, reaction: Optional[str]): ...
    async def fetch(self) -> MessageData: ...
    @staticmethod
    def format_mentions(text, *args, **kwargs): ...

class MessageSnippet(Message):
    author: str
    created_at: datetime.datetime
    text: str
    matched_keywords: Mapping[int, str]

class MessageData(Message):
    author: str
    created_at: datetime.datetime
    text: Optional[str]
    mentions: Sequence[Mention]
    emoji_size: Optional[EmojiSize]
    is_read: Optional[bool]
    read_by: bool
    reactions: Mapping[str, str]
    sticker: Optional[_sticker.Sticker]
    attachments: Sequence[_attachment.Attachment]
    quick_replies: Sequence[_quick_reply.QuickReply]
    unsent: Optional[bool]
    reply_to_id: Optional[str]
    replied_to: Optional[Any]
    forwarded: Optional[bool]

def graphql_to_extensible_attachment(data): ...
