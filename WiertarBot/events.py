from typing import Optional, Protocol

from pl.kvgx12.wiertarbot.connector import FileData as KtFileData, UploadedFile as KtUploadedFile, \
    ThreadData as KtThreadData
from pl.kvgx12.wiertarbot.events import Mention as KtMention, MessageEvent as KtMessageEvent


class ThreadData(Protocol):
    def __new__(cls, id: str, name: str, message_count: Optional[int], participants: list[str]) -> 'ThreadData':
        return KtThreadData(id, name, message_count, participants)

    def getId(self) -> str: ...

    def getName(self) -> str: ...

    def getMessageCount(self) -> Optional[int]: ...

    def getParticipants(self) -> list[str]: ...


class Mention(Protocol):
    def __new__(cls, thread_id: str, offset: int, length: int) -> 'Mention':
        return KtMention(thread_id, offset, length)

    def getThreadId(self) -> str: ...

    def getOffset(self) -> int: ...

    def getLength(self) -> int: ...


class FileData(Protocol):
    def __new__(cls, uri: str, content: bytes, media_type: str) -> 'FileData':
        return KtFileData(uri, content, media_type)

    def getUri(self) -> str: ...

    def getContent(self) -> bytes: ...

    def getMediaType(self) -> str: ...


class UploadedFile(Protocol):
    def __new__(cls, id: str, mime_type: str) -> 'UploadedFile':
        return KtUploadedFile(id, mime_type)

    def getId(self) -> str: ...

    def getMimeType(self) -> str: ...


class Attachment(Protocol):
    def getId(self) -> Optional[str]: ...


class ImageAttachment(Attachment):
    def getWidth(self) -> Optional[int]: ...

    def getHeight(self) -> Optional[int]: ...

    def getOriginalExtension(self) -> Optional[str]: ...

    def isAnimated(self) -> Optional[bool]: ...


class Context(Protocol): ...

class MessageEvent(Protocol):
    def __new__(cls,
                context: Context,
                text: str,
                author_id: str,
                thread_id: str,
                at: int,
                mentions: list[Mention],
                external_id: str,
                reply_to_id: Optional[str],
                attachments: list[Attachment],
                ) -> 'MessageEvent':
        return KtMessageEvent(context, text, author_id, thread_id, at, mentions, external_id, reply_to_id, attachments)

    def getContext(self) -> Context: ...

    def getText(self) -> str: ...

    def getAuthorId(self) -> str: ...

    def getThreadId(self) -> str: ...

    def getAt(self) -> int: ...

    def getMentions(self) -> list[Mention]: ...

    def getExternalId(self) -> str: ...

    def getReplyToId(self) -> Optional[str]: ...

    def getAttachments(self) -> list[Attachment]: ...

    def isGroup(self) -> bool: ...

    async def pyReact(self, reaction: str) -> None: ...
