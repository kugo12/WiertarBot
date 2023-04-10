import mimetypes
from io import BytesIO
from os import path
from typing import Optional, Iterable, Union, cast, TypeAlias

import aiofiles
import aiohttp

from .generic import fb_attachment_to_generic
from ... import config, fbchat
from ...abc import PyContext
from ...response import IResponse
from ...events import MessageEvent, Mention, FileData, UploadedFile, ThreadData, ByteArray


def fb_mentions(mentions: Iterable['Mention'] | None) -> list[fbchat.Mention]:
    return [
        fbchat.Mention(thread_id=it.getThreadId(), offset=it.getOffset(), length=it.getLength())
        for it in mentions
    ] if mentions else []


def jbyte_array_to_bytes(arr: ByteArray) -> bytes:  # jvm bytes are signed
    return b"".join(it.to_bytes(length=1, byteorder="big", signed=True) for it in arr)


FBThreadData: TypeAlias = fbchat.UserData | fbchat.GroupData | fbchat.PageData


class FBContext(PyContext):
    def __init__(self, client: fbchat.Client) -> None:
        self.__client = client
        self.__session = client.session

    @property
    def bot_id(self) -> str:
        return self.__client.session.user.id

    def get_bot_id(self) -> str:
        return self.bot_id

    def _get_event_thread(self, event: MessageEvent) -> fbchat.ThreadABC:
        t: type[Union[fbchat.Group, fbchat.User]] = fbchat.Group if event.isGroup() else fbchat.User

        return t(session=self.__session, id=event.getThreadId())

    async def send_response(self, response: IResponse) -> None:
        thread = self._get_event_thread(response.getEvent())
        text = response.getText()
        files = [(it.getId(), it.getMimeType()) for it in response.getFiles()] if response.getFiles() else None

        if text:
            await thread.send_text(
                text=text,
                mentions=fb_mentions(response.getMentions()),
                files=files,
                reply_to_id=cast(str, response.getReplyToId())
            )
        elif files:
            await thread.send_files(files)

    async def upload_raw(
            self, files: list[FileData], voice_clip: bool = False
    ) -> list[UploadedFile]:
        return [
            UploadedFile.new(it[0], it[1])
            for it in await self.__client.upload(
                [(it.getUri(), BytesIO(jbyte_array_to_bytes(it.getContent())), it.getMediaType()) for it in files],
                voice_clip
            )
        ]

    async def fetch_thread(self, id: str) -> ThreadData:
        data: FBThreadData = await self.__client.fetch_thread_info([id]).__anext__()  # type: ignore

        return ThreadData.new(
            id=data.id,
            name=data.name or "",
            message_count=data.message_count,
            participants=[it.id for it in data.participants] if isinstance(data, fbchat.GroupData) else []
        )

    async def fetch_image_url(self, image_id: str) -> str:
        return await self.__client.fetch_image_url(image_id)

    async def send_text(self, event: MessageEvent, text: str) -> None:
        thread = self._get_event_thread(event)
        await thread.send_text(text=text)

    async def react_to_message(self, event: MessageEvent, reaction: Optional[str]) -> None:
        thread = self._get_event_thread(event)
        await fbchat.Message(thread=thread, id=event.getExternalId()).react(reaction)

    async def fetch_replied_to(self, event: MessageEvent) -> Optional[MessageEvent]:
        if event.getReplyToId() is None:
            return None

        thread = self._get_event_thread(event)

        message = await fbchat.Message(thread=thread, id=event.getReplyToId()).fetch()

        return MessageEvent.new(
            context=event.getContext(),
            text=message.text or "",
            author_id=message.author,
            thread_id=message.thread.id,
            at=int(message.created_at.timestamp()),
            mentions=[Mention.new(it.thread_id, it.offset, it.length) for it in message.mentions],
            external_id=message.id,
            reply_to_id=message.reply_to_id,
            attachments=[fb_attachment_to_generic(it) for it in message.attachments]
        )

    async def save_attachment(self, attachment) -> None:
        name = type(attachment).__name__
        if name in ('AudioAttachment', 'ImageAttachment', 'VideoAttachment'):
            if name == 'AudioAttachment':
                url = attachment.url
                p = str(config.attachment_save_path / attachment.filename)
            elif name == 'ImageAttachment':
                url = await self.fetch_image_url(attachment.id)
                p = str(config.attachment_save_path / f'{attachment.id}.{attachment.original_extension}')
            else:  # 'VideoAttachment'
                url = attachment.preview_url
                p = str(config.attachment_save_path / f'{attachment.id}.mp4')

            async with self.__client.session._session.get(url) as r:
                if r.status == 200:
                    async with aiofiles.open(p, mode='wb') as f:
                        await f.write(await r.read())

    async def upload(self, files: list[str], voice_clip=False) -> Optional[list[UploadedFile]]:
        final_files: list[FileData] = []
        for fn in files:
            if path.exists(fn):
                mime, _ = mimetypes.guess_type(fn)
                with open(fn, 'rb') as file:
                    f = file.read()
                true_fn = path.basename(fn)

                if mime == 'video/mp4' and voice_clip:
                    mime = 'audio/mp4'

                final_files.append(FileData.new(true_fn, f, mime))
            elif fn.startswith(('http://', 'https://')):
                async with aiohttp.ClientSession() as session:
                    async with session.get(fn) as r:
                        if r.status == 200:
                            final_files.append(FileData.new(fn, await r.read(), r.content_type))

        if final_files:
            return await self.upload_raw(final_files, voice_clip)
        return None