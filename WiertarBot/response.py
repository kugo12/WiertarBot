from typing import Optional, Union

from WiertarBot.events import MessageEvent, Mention


class Response:
    __slots__ = ["event", "text", "files", "voice_clip", "mentions", "reply_to_id"]

    def __init__(
            self,
            event: MessageEvent,
            *,
            text: Optional[str] = None,
            files: Optional[Union[list[str], list[tuple[str, str]]]] = None,
            voice_clip: bool = False,
            mentions: Optional[list[Mention]] = None,
            reply_to_id: Optional[str] = None
            ):
        self.event = event
        self.text = text
        self.files = files
        self.voice_clip = voice_clip
        self.mentions = mentions
        self.reply_to_id = reply_to_id

    async def send(self):
        await self.event.context.send_response(self)
