from abc import ABC, abstractmethod
from typing import Optional, Sequence, Tuple, Iterable, BinaryIO, Union, TYPE_CHECKING

import fbchat

if TYPE_CHECKING:
    from ..response import IResponse
    from ..events import MessageEvent

ThreadData = Union[fbchat.UserData, fbchat.GroupData, fbchat.PageData]


class PyContext(ABC):
    @abstractmethod
    async def send_response(self, response: 'IResponse') -> None: ...

    @abstractmethod
    async def upload_raw(
            self, files: Iterable[Tuple[str, BinaryIO, str]], voice_clip: bool = False
    ) -> list[Tuple[str, str]]: ...

    @abstractmethod
    async def fetch_thread(self, id: str) -> ThreadData: ...  # FIXME

    @abstractmethod
    async def fetch_image_url(self, image_id: str) -> str: ...

    @abstractmethod
    async def send_text(self, event: 'MessageEvent', text: str) -> None: ...

    @abstractmethod
    async def react_to_message(self, event: 'MessageEvent', reaction: Optional[str]) -> None: ...

    @abstractmethod
    async def fetch_replied_to(self, event: 'MessageEvent') -> Optional[fbchat.MessageData]: ...  # FIXME

    @abstractmethod
    async def save_attachment(self, attachment) -> None: ...

    @abstractmethod
    async def upload(self, files: Iterable[str], voice_clip=False) -> Optional[Sequence[Tuple[str, str]]]: ...
