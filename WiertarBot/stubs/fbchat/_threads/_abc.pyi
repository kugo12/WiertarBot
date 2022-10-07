import abc
import datetime
from .. import _models, _session
from .._common import attrs_default as attrs_default, log as log
from _typeshed import Incomplete
from typing import AsyncGenerator, AsyncIterator, Iterable, Mapping, Optional, Tuple

DEFAULT_COLOR: str
SETABLE_COLORS: Incomplete

class ThreadABC(metaclass=abc.ABCMeta):
    @property
    @abc.abstractmethod
    def session(self) -> _session.Session: ...
    @property
    @abc.abstractmethod
    def id(self) -> str: ...
    async def fetch(self) -> None: ...
    async def wave(self, first: bool = ...) -> str: ...
    async def send_text(self, text: str, mentions: Iterable['_models.Mention'] = ..., files: Iterable[Tuple[str, str]] = ..., reply_to_id: str = ...) -> str: ...
    async def send_emoji(self, emoji: str, size: _models.EmojiSize) -> str: ...
    async def send_sticker(self, sticker_id: str) -> str: ...
    async def send_location(self, latitude: float, longitude: float): ...
    async def send_pinned_location(self, latitude: float, longitude: float): ...
    async def send_files(self, files: Iterable[Tuple[str, str]]): ...
    async def search_messages(self, query: str, limit: int) -> AsyncIterator[_models.MessageSnippet]: ...
    async def fetch_messages(self, limit: Optional[int]) -> AsyncGenerator['_models.MessageData', None]: ...
    async def fetch_images(self, limit: Optional[int]) -> AsyncIterator['_models.Attachment']: ...
    async def set_nickname(self, user_id: str, nickname: str): ...
    async def set_color(self, color: str): ...
    async def set_emoji(self, emoji: Optional[str]): ...
    async def forward_attachment(self, attachment_id: str): ...
    async def start_typing(self) -> None: ...
    async def stop_typing(self) -> None: ...
    async def create_plan(self, name: str, at: datetime.datetime, location_name: str = ..., location_id: str = ...): ...
    async def create_poll(self, question: str, options: Mapping[str, bool]): ...
    async def mute(self, duration: datetime.timedelta = ...): ...
    async def unmute(self): ...
    async def mute_reactions(self) -> None: ...
    async def unmute_reactions(self) -> None: ...
    async def mute_mentions(self) -> None: ...
    async def unmute_mentions(self) -> None: ...
    async def mark_as_spam(self) -> None: ...
    async def delete(self) -> None: ...

class Thread(ThreadABC):
    session: _session.Session
    id: str