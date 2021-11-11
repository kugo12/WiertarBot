from typing import List

import fbchat

from WiertarBot import bot


class Response:
    __slots__ = ["event", "text", "files", "voice_clip", "mentions", "reply_to_id"]

    def __init__(
            self,
            event: fbchat.MessageEvent,
            *,
            text: str = None,
            files: List[str] = None,
            voice_clip: bool = False,
            mentions: List[fbchat.Mention] = None,
            reply_to_id: str = None
            ):
        self.event = event
        self.text = text
        self.files = files
        self.voice_clip = voice_clip
        self.mentions = mentions
        self.reply_to_id = reply_to_id

    async def send(self) -> str:
        if self.files:
            if isinstance(self.files[0], str):
                self.files = await bot.WiertarBot.upload(self.files, self.voice_clip)

        return await self.event.thread.send_text(text=self.text, mentions=self.mentions,
                                                 files=self.files, reply_to_id=self.reply_to_id)
