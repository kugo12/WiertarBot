import fbchat
import aiohttp
from abc import ABC, abstractmethod
from io import BytesIO
from typing import BinaryIO, Optional, final, Final

from .. import bot
from ..events import MessageEvent
from ..response import Response


class ImageEditABC(ABC):
    __slots__ = ['args']
    mime = 'image/jpeg'
    fn = 'imageedit.jpg'

    def __init__(self, args: str):
        super().__init__()

        self.args = args.split(' ')

    @abstractmethod
    async def edit(self, fp: BinaryIO) -> BinaryIO:
        pass

    async def get_image_from_attachments(self, event: MessageEvent, msg: fbchat.MessageData) -> Optional[BinaryIO]:
        f = None

        if msg.attachments:
            if isinstance(msg.attachments[0], fbchat.ImageAttachment):
                image = msg.attachments[0]
                if image.id is None:
                    return None
                url = await event.context.fetch_image_url(image.id)

                async with aiohttp.ClientSession() as session:
                    async with session.get(url) as r:
                        if r.status == 200:
                            f = BytesIO(await r.read())

        return f

    @final
    async def edit_and_send(self, event: MessageEvent, fp: BinaryIO):
        f = await self.edit(fp)
        file = await event.context.upload_raw([(self.fn, f, self.mime)])

        await event.send_response(files=file)

    @final
    async def check(self, event: MessageEvent) -> bool:
        replied_to = await event.context.fetch_replied_to(event)
        if replied_to:
            f = await self.get_image_from_attachments(event, replied_to)
            if f:
                await self.edit_and_send(event, f)
                return False

        await event.send_response(text="Wyślij zdjęcie")
        return True
