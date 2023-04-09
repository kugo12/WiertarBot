from typing import Optional, Protocol, Iterator, Iterable, Self, cast

from pl.kvgx12.wiertarbot.connector import FileData as KtFileData, UploadedFile as KtUploadedFile, \
    ThreadData as KtThreadData
from pl.kvgx12.wiertarbot.events import \
    Mention as KtMention, \
    MessageEvent as KtMessageEvent, \
    Attachment as KtAttachment, \
    ImageAttachment as KtImageAttachment


class ByteArray(Protocol, Iterable[int]): ...


class ThreadData(Protocol):
    @staticmethod
    def new(id: str, name: str, message_count: Optional[int], participants: list[str]) -> 'ThreadData':
        return cast(ThreadData, KtThreadData(id, name, message_count, participants))

    def getId(self) -> str: ...

    def getName(self) -> str: ...

    def getMessageCount(self) -> Optional[int]: ...

    def getParticipants(self) -> list[str]: ...


class Mention(Protocol):
    @staticmethod
    def new(thread_id: str, offset: int, length: int) -> 'Mention':
        return cast(Mention, KtMention(thread_id, offset, length))

    def getThreadId(self) -> str: ...

    def getOffset(self) -> int: ...

    def getLength(self) -> int: ...


class FileData(Protocol):
    @staticmethod
    def new(uri: str, content: bytes, media_type: str) -> 'FileData':
        return cast(FileData, KtFileData(uri, content, media_type))

    def getUri(self) -> str: ...

    def getContent(self) -> ByteArray: ...

    def getMediaType(self) -> str: ...


class UploadedFile(Protocol):
    @staticmethod
    def new(id: str, mime_type: str) -> 'UploadedFile':
        return cast(UploadedFile, KtUploadedFile(id, mime_type))

    def getId(self) -> str: ...

    def getMimeType(self) -> str: ...


class Attachment(Protocol):
    @staticmethod
    def new(id: str) -> 'Attachment':
        return cast(Attachment, KtAttachment(id))

    def getId(self) -> Optional[str]: ...


class ImageAttachment(Attachment):
    @staticmethod
    def new(  # type: ignore[override]
            id: Optional[str],
            width: Optional[int],
            height: Optional[int],
            original_extension: Optional[str],
            is_animated: Optional[bool]
    ) -> 'ImageAttachment':
        return cast(ImageAttachment, KtImageAttachment(id, width, height, original_extension, is_animated))

    def getId(self) -> Optional[str]: ...

    def getWidth(self) -> Optional[int]: ...

    def getHeight(self) -> Optional[int]: ...

    def getOriginalExtension(self) -> Optional[str]: ...

    def isAnimated(self) -> Optional[bool]: ...


class Context(Protocol): ...


class MessageEvent(Protocol):
    @staticmethod
    def new(
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
        return cast(
            MessageEvent,
            KtMessageEvent(context, text, author_id, thread_id, at, mentions, external_id, reply_to_id, attachments)
        )

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
