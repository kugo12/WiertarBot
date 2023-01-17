from typing import Optional, Iterable, TYPE_CHECKING, Generic, TypeVar, final

from .models import Session, Permission, FBMessage, MessageCountMilestone
from sqlalchemy.sql import update, select, delete
from abc import ABCMeta
from functools import cache

_T = TypeVar("_T")


class BaseRepository(Generic[_T], metaclass=ABCMeta):
    @classmethod
    @final
    def save(cls, obj: _T) -> None:
        with Session.begin() as session:
            session.add(obj)


# noinspection PyComparisonWithNone
class FBMessageRepository(BaseRepository[FBMessage]):
    @classmethod
    def mark_deleted(cls, mid: str, timestamp: int) -> None:
        with Session.begin() as session:
            session.execute(update(FBMessage)
                            .where(FBMessage.message_id == mid)
                            .values(deleted_at=timestamp))

    @classmethod
    def find_not_deleted_and_time_before(cls, time: int) -> Iterable[FBMessage]:
        with Session() as session:
            return session.scalars(select(FBMessage).where(
                FBMessage.time < time,
                FBMessage.deleted_at == None
            )).all()

    @classmethod
    def remove_not_deleted_and_time_before(cls, time: int) -> None:
        with Session.begin() as session:
            session.execute(delete(FBMessage).where(
                FBMessage.time < time,
                FBMessage.deleted_at == None
            ))

    @classmethod
    def find_deleted_by_thread_id(cls, thread_id: str, limit: int) -> Iterable[FBMessage]:
        with Session() as session:
            return session.scalars(select(FBMessage).where(
                FBMessage.deleted_at != None,
                FBMessage.thread_id == thread_id,
            ).order_by(FBMessage.time.desc())).all()


class PermissionRepository(BaseRepository[Permission]):
    @classmethod
    @cache
    def find_by_command(cls, command: str) -> Optional[Permission]:
        with Session() as session:
            return session.scalar(select(Permission).where(
                Permission.command == command
            ))


class MilestoneMessageCountRepository(BaseRepository[MessageCountMilestone]):
    @classmethod
    def find_by_thread_id(cls, thread_id: str) -> Optional[MessageCountMilestone]:
        with Session() as session:
            return session.scalar(select(MessageCountMilestone).where(
                MessageCountMilestone.thread_id == thread_id
            ))
