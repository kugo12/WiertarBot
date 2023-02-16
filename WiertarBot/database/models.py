from typing import Optional, Protocol
from pl.kvgx12.wiertarbot.entities import \
    FBMessage as _FBMessage, \
    MessageCountMilestone as _MessageCountMilestone, \
    Permission as _Permission


class Permission(Protocol):
    @staticmethod
    def new(command: str, whitelist: str, blacklist: str) -> 'Permission':
        return _Permission(None, command, whitelist, blacklist)


    def getId(self) -> int: ...

    def getCommand(self) -> str: ...

    def getWhitelist(self) -> str: ...

    def getBlacklist(self) -> str: ...


class FBMessage(Protocol):
    @staticmethod
    def new(message_id: str, thread_id: str, author_id: str, time: int, message: str,
            deleted_at: Optional[int] = None) -> 'FBMessage':
        return _FBMessage(None, message_id, thread_id, author_id, time, message, deleted_at)

    def getId(self) -> int: ...

    def getMessageId(self) -> str: ...

    def getThreadId(self) -> str: ...

    def getAuthorId(self) -> str: ...

    def getTime(self) -> int: ...

    def getMessage(self) -> str: ...

    def getDeletedAt(self) -> Optional[int]: ...

    def setDeletedAt(self, deletedAt: int) -> None: ...


class MessageCountMilestone(Protocol):
    @staticmethod
    def new(thread_id: str, count: int) -> 'MessageCountMilestone':
        return _MessageCountMilestone(None, thread_id, count)

    def getId(self) -> int: ...

    def getThreadId(self) -> str: ...

    def getCount(self) -> int: ...

    def setCount(self, count: int) -> None: ...
