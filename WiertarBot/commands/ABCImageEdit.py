import fbchat
import aiohttp
from abc import ABC, abstractmethod
from io import BytesIO
from typing import BinaryIO, Optional, final

from ..events import MessageEvent, ImageAttachment, Attachment
from ..response import response

from pl.kvgx12.wiertarbot.events import Attachment as KtAttachment, ImageAttachment as KtImageAttachment


def fb_attachment_to_generic(attachment: fbchat.Attachment) -> Attachment:
    if isinstance(attachment, fbchat.ImageAttachment):
        return KtImageAttachment(
            id=attachment.id,
            width=attachment.width,
            height=attachment.height,
            original_extension=attachment.original_extension,
            is_animated=attachment.is_animated
        )
    return KtAttachment(attachment.id)

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
        file = await event.getContext().pyUploadRaw([(self.fn, f, self.mime)])

        await response(event, files=file).pySend()

    @final
    async def check(self, event: MessageEvent) -> bool:
        replied_to = await event.getContext().pyFetchRepliedTo(event)  # FIXME
        if replied_to:
            f = await self.get_image_from_attachments(event, [fb_attachment_to_generic(it) for it in replied_to.attachments])
            if f:
                await self.edit_and_send(event, f)
                return False

        await response(event, text="Wyślij zdjęcie").pySend()
        return True
