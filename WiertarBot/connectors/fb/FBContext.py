import mimetypes
from io import BytesIO
from os import path
from typing import Optional, Sequence, Tuple, Iterable, BinaryIO, Union, Final, cast, TYPE_CHECKING

import aiofiles
import aiohttp
import fbchat

from ... import config
from ...abc.context import PyContext, ThreadData

if TYPE_CHECKING:
    from ...response import IResponse
    from ...events import MessageEvent, Mention


def fb_mentions(mentions: Iterable['Mention']) -> list[fbchat.Mention]:
    return [
        fbchat.Mention(thread_id=it.getThreadId(), offset=it.getOffset(), length=it.getLength())
        for it in mentions
    ] if mentions else []


class FBContext(PyContext):
    def __init__(self, client: fbchat.Client) -> None:
        self.__client = client
        self.__session = client.session

    @property
    def bot_id(self) -> str:
        return self.__client.session.user.id

    def get_bot_id(self) -> str:
        return self.bot_id

    def _get_event_thread(self, event: 'MessageEvent') -> fbchat.ThreadABC:
        t: type[Union[fbchat.Group, fbchat.User]] = fbchat.Group if event.isGroup() else fbchat.User

        return t(session=self.__session, id=event.getThreadId())

    async def _resolve_response_files(self, response: 'IResponse') -> Optional[Sequence[tuple[str, str]]]:
        files = response.getFiles()
        if files:
            if isinstance(files[0], str):
                return await self.upload(cast(list[str], files), response.getVoiceClip())
            return cast(list[tuple[str, str]], files)
        return None

    async def send_response(self, response: 'IResponse') -> None:
        files: Final = await self._resolve_response_files(response)

        thread = self._get_event_thread(response.getEvent())
        text = response.getText()

        if text:
            await thread.send_text(
                text=text,
                mentions=fb_mentions(response.getMentions()),
                files=cast(list[tuple[str, str]], files),
                reply_to_id=cast(str, response.getReplyToId())
            )
        elif files:
            await thread.send_files(files)

    async def upload_raw(
            self, files: Iterable[Tuple[str, BinaryIO, str]], voice_clip: bool = False
    ) -> list[Tuple[str, str]]:
        return list(await self.__client.upload(files, voice_clip))

    async def fetch_thread(self, id: str) -> ThreadData:
        data = await self.__client.fetch_thread_info([id]).__anext__()  # type: ignore

        return cast(ThreadData, data)

    async def fetch_image_url(self, image_id: str) -> str:
        return await self.__client.fetch_image_url(image_id)

    async def send_text(self, event: 'MessageEvent', text: str) -> None:
        thread = self._get_event_thread(event)
        await thread.send_text(text=text)

    async def react_to_message(self, event: 'MessageEvent', reaction: Optional[str]) -> None:
        thread = self._get_event_thread(event)
        await fbchat.Message(thread=thread, id=event.getExternalId()).react(reaction)

    async def fetch_replied_to(self, event: 'MessageEvent') -> Optional[fbchat.MessageData]:
        if event.getReplyToId() is None:
            return None

        thread = self._get_event_thread(event)

        return await fbchat.Message(thread=thread, id=event.getReplyToId()).fetch()

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

    async def upload(self, files: Iterable[str], voice_clip=False) -> Optional[Sequence[Tuple[str, str]]]:
        final_files = []
        for fn in files:
            if fn.startswith(('http://', 'https://')):
                true_fn: str = path.basename(fn.split('?', 1)[0])  # without get params
                mime, _ = mimetypes.guess_type(true_fn)
            else:
                mime, _ = mimetypes.guess_type(fn)

            if mime:
                if path.exists(fn):
                    with open(fn, 'rb') as file:
                        f = BytesIO(file.read())
                    true_fn = path.basename(fn)

                    if mime == 'video/mp4' and voice_clip:
                        mime = 'audio/mp4'

                    final_files.append((true_fn, f, mime))

                elif fn.startswith(('http://', 'https://')):
                    async with aiohttp.ClientSession() as session:
                        async with session.get(fn) as r:
                            if r.status == 200:
                                f = BytesIO(await r.read())
                                final_files.append((true_fn, f, mime))

        if final_files:
            uploaded = await self.upload_raw(final_files, voice_clip)
        else:
            return None

        return uploaded
