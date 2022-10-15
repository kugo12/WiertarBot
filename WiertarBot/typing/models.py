from typing import Optional

from ..database import models


class Permission(models.Permission):
    id: Optional[int]
    command: str  # type: ignore[assignment]
    whitelist: str  # type: ignore[assignment]
    blacklist: str  # type: ignore[assignment]

    @classmethod
    def new(cls, command: str, whitelist: str, blacklist: str) -> 'Permission':
        return cls(command=command, whitelist=whitelist, blacklist=blacklist)


class FBMessage(models.FBMessage):
    id: Optional[int]
    message_id: str  # type: ignore[assignment]
    thread_id: str  # type: ignore[assignment]
    author_id: str  # type: ignore[assignment]
    time: int  # type: ignore[assignment]
    message: str  # type: ignore[assignment]
    deleted_at: str  # type: ignore[assignment]

    @classmethod
    def new(
            cls,
            message_id: str,
            thread_id: str,
            author_id: str,
            time: int,
            message: str,
            deleted_at: str,
    ) -> 'FBMessage':
        return cls(
            message_id=message_id,
            thread_id=thread_id,
            author_id=author_id,
            time=time,
            message=message,
            deleted_at=deleted_at,
        )


class MessageCountMilestone(models.MessageCountMilestone):
    id: Optional[int]
    thread_id: str  # type: ignore[assignment]
    count: int  # type: ignore[assignment]

    @classmethod
    def new(cls, thread_id: str, count: int) -> 'MessageCountMilestone':
        return cls(
            thread_id=thread_id,
            count=count
        )
