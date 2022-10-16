from typing import Optional, Union, TYPE_CHECKING

import fbchat

if TYPE_CHECKING:
    from .events import MessageEvent, Mention


class Response:
    __slots__ = ["event", "text", "files", "voice_clip", "mentions", "reply_to_id"]

    def __init__(
            self,
            event: 'MessageEvent',
            *,
            text: Optional[str] = None,
            files: Optional[Union[list[str], list[tuple[str, str]]]] = None,
            voice_clip: bool = False,
            mentions: Optional[list['Mention']] = None,
            reply_to_id: Optional[str] = None
            ) -> None:
        self.event = event
        self.text = text
        self.files = files
        self.voice_clip = voice_clip
        self.mentions = mentions
        self.reply_to_id = reply_to_id

    async def send(self) -> None:
        await self.event.context.send_response(self)

    @property
    def fb_mentions(self) -> list[fbchat.Mention]:
        return [it.fb for it in self.mentions] if self.mentions else []
