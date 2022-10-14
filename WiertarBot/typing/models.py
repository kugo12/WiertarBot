from typing import Optional

from ..database import models


class QueriedPermission(models.Permission):
    id: Optional[int]
    command: str  # type: ignore[assignment]
    whitelist: str  # type: ignore[assignment]
    blacklist: str  # type: ignore[assignment]

    @classmethod
    def new(cls, command: str, whitelist: str, blacklist: str) -> 'QueriedPermission':
        return cls(command=command, whitelist=whitelist, blacklist=blacklist)


class QueriedFBMessage(models.FBMessage):
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
    ) -> 'QueriedFBMessage':
        return cls(
            message_id=message_id,
            thread_id=thread_id,
            author_id=author_id,
            time=time,
            message=message,
            deleted_at=deleted_at,
        )
