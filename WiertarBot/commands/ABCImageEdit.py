import fbchat
import aiohttp
from abc import ABC, abstractmethod
from io import BytesIO
from typing import BinaryIO, Optional, final

from ..events import MessageEvent, ImageAttachment, Attachment


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

    async def get_image_from_attachments(self, event: MessageEvent, attachments: list[Attachment]) -> Optional[BinaryIO]:
        if attachments and isinstance(attachments[0], ImageAttachment):
            image = attachments[0]
            if image.id is None:
                return None
            url = await event.context.fetch_image_url(image.id)

            async with aiohttp.ClientSession() as session:
                async with session.get(url) as r:
                    if r.status == 200:
                        return BytesIO(await r.read())
        return None

    @final
    async def edit_and_send(self, event: MessageEvent, fp: BinaryIO):
        f = await self.edit(fp)
        file = await event.context.upload_raw([(self.fn, f, self.mime)])

        await event.send_response(files=file)

    @final
    async def check(self, event: MessageEvent) -> bool:
        replied_to = await event.context.fetch_replied_to(event)  # FIXME
        if replied_to:
            f = await self.get_image_from_attachments(event, [Attachment.from_fb(it) for it in replied_to.attachments])
            if f:
                await self.edit_and_send(event, f)
                return False

        await event.send_response(text="Wyślij zdjęcie")
        return True
