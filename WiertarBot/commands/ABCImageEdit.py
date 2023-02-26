import aiohttp
from abc import ABC, abstractmethod
from io import BytesIO
from typing import BinaryIO, Optional, final

from ..events import MessageEvent, ImageAttachment, Attachment, FileData
from ..response import response


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

    async def get_image_from_attachments(
            self,
            event: MessageEvent,
            attachments: list[Attachment]
    ) -> Optional[BinaryIO]:
        if attachments and isinstance(attachments[0], ImageAttachment):
            image = attachments[0]
            if image.getId() is None:
                return None
            url = await event.getContext().pyFetchImageUrl(image.getId())

            async with aiohttp.ClientSession() as session:
                async with session.get(url) as r:
                    if r.status == 200:
                        return BytesIO(await r.read())
        return None

    @final
    async def edit_and_send(self, event: MessageEvent, fp: BinaryIO):
        f = await self.edit(fp)
        file = await event.getContext().pyUploadRaw([FileData(self.fn, f.read(), self.mime)])

        await response(event, files=file).pySend()

    @final
    async def check(self, event: MessageEvent) -> bool:
        replied_to = await event.getContext().pyFetchRepliedTo(event)
        if replied_to:
            f = await self.get_image_from_attachments(event, replied_to.getAttachments())
            if f:
                await self.edit_and_send(event, f)
                return False

        await response(event, text="Wyślij zdjęcie").pySend()
        return True
