from datetime import datetime
from typing import Optional

import attr
import fbchat

from WiertarBot.context import Context
from WiertarBot.response import Response
from WiertarBot.typing import FBMessageEvent


@attr.s(frozen=True, init=True, auto_attribs=True)
class Mention:
    thread_id: str
    offset: int
    length: int

    @classmethod
    def from_fb(cls, mention: fbchat.Mention) -> 'Mention':
        return cls(
            thread_id=mention.thread_id,
            offset=mention.offset,
            length=mention.length
        )


@attr.s(frozen=True, init=True, auto_attribs=True)
class MessageEvent:
    context: Context
    text: str
    author_id: str
    thread_id: str
    at: datetime
    mentions: list[Mention]
    external_id: str
    reply_to_id: Optional[str]

    @property
    def is_group(self):
        return self.thread_id != self.author_id

    async def react(self, reaction: str):
        await self.context.react_to_message(self, reaction)

    async def send_response(self, **kwargs):
        await Response(event=self, **kwargs).send()

    @classmethod
    def from_fb_event(cls, context: Context, event: FBMessageEvent) -> 'MessageEvent':
        text: str = event.message.text or ""

        return cls(
            context=context,
            text=text,
            author_id=event.author.id,
            thread_id=event.thread.id,
            at=event.message.created_at,
            mentions=[Mention.from_fb(it) for it in event.message.mentions],
            external_id=event.message.id,
            reply_to_id=event.message.reply_to_id,
        )

    # FIXME: remove this hack
    @classmethod
    def copy_with_different_text(cls, event: 'MessageEvent', text: str) -> 'MessageEvent':
        return cls(
            context=event.context,
            text=text,
            author_id=event.author_id,
            thread_id=event.thread_id,
            at=event.at,
            mentions=event.mentions,
            external_id=event.external_id,
            reply_to_id=event.reply_to_id
        )

