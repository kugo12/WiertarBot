import fbchat
import aiohttp
from abc import ABC, abstractmethod
from io import BytesIO
from typing import BinaryIO

from .. import bot


class ImageEditABC(ABC):
    __slots__ = ['args']
    mime = 'image/jpeg'
    fn = 'imageedit.jpg'

    def __init__(self):
        super().__init__()

    @abstractmethod
    async def edit(self, fp: BinaryIO) -> BinaryIO:
        pass

    async def get_profile_picture(self, uid: str) -> BinaryIO:
        url = f'https://graph.facebook.com/v3.1/{ uid }/picture?height=500'
        f = None

        async with aiohttp.ClientSession() as session:
            async with session.get(url) as r:
                if r.status == 200:
                    f = BytesIO(await r.read())

        return f

    async def get_image_from_attachments(self, msg: fbchat.MessageData) -> BinaryIO:
        f = None

        if msg.attachments:
            if isinstance(msg.attachments[0], fbchat.ImageAttachment):
                image = msg.attachments[0]
                url = await bot.WiertarBot.client.fetch_image_url(image.id)

                async with aiohttp.ClientSession() as session:
                    async with session.get(url) as r:
                        if r.status == 200:
                            f = BytesIO(await r.read())

        return f

    async def edit_and_send(self, event: fbchat.MessageEvent, fp: BinaryIO):
        f = await self.edit(fp)
        f = await bot.WiertarBot.client.upload([(self.fn, f, self.mime)])
        await event.thread.send_files(f)

    async def check(self, event: fbchat.MessageEvent):
        msg = event.message
        self.args = msg.text.split(' ')

        if msg.reply_to_id:
            replied_to = fbchat.Message(thread=event.thread, id=msg.reply_to_id)
            replied_to = await replied_to.fetch()

            f = await self.get_image_from_attachments(replied_to)
            if f:
                await self.edit_and_send(event, f)
                return False

        if msg.mentions:
            mnt = msg.mentions[0]
            name = msg.text[mnt.offset:mnt.offset+mnt.length].count(' ')
            del self.args[1: name+1]

            f = await self.get_profile_picture(mnt.thread_id)
            await self.edit_and_send(event, f)
            return False

        if self.args[1].lower() == '@me':
            f = await self.get_profile_picture(event.author.id)
            await self.edit_and_send(event, f)
            return False

        await event.thread.send_text(text="Wyślij zdjęcie")
        return True
