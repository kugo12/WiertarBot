from typing import Optional, Iterable, TYPE_CHECKING

from .models import db, Permission, FBMessage

if TYPE_CHECKING:
    from ..typing import QueriedFBMessage, QueriedPermission

# noinspection PyComparisonWithNone
class FBMessageRepository:
    @staticmethod
    @db.atomic()
    def save(obj: FBMessage, *, force_insert=False) -> None:
        obj.save(force_insert=force_insert)

    @staticmethod
    @db.atomic()
    def mark_deleted(mid: str, timestamp: int) -> None:
        (
            FBMessage
            .update(deleted_at=timestamp)
            .where(FBMessage.message_id == mid)
            .execute()
        )

    @staticmethod
    def find_not_deleted_and_time_before(time: int) -> Iterable['QueriedFBMessage']:
        return (
            FBMessage
            .select(FBMessage.message)
            .whee(FBMessage.time < time, FBMessage.deleted_at == None)
        )

    @staticmethod
    @db.atomic()
    def remove_not_deleted_and_time_before(time: int) -> int:
        return (
            FBMessage
            .delete()
            .where(FBMessage.time < time, FBMessage.deleted_at == None)
            .execute()
        )

    @staticmethod
    def find_deleted_by_thread_id(thread_id: str, limit: int) -> Iterable['QueriedFBMessage']:
        return (
            FBMessage
            .select(FBMessage.message)
            .where(
                FBMessage.deleted_at != None,
                FBMessage.thread_id == thread_id
            )
            .order_by(FBMessage.time.desc())
            .limit(limit)
        )


class PermissionRepository:
    @staticmethod
    def find_by_command(command: str) -> Optional['QueriedPermission']:
        return Permission.get_or_none(Permission.command == command)

    @staticmethod
    @db.atomic()
    def save(obj: Permission, *, force_insert=False) -> None:
        obj.save(force_insert=force_insert)
