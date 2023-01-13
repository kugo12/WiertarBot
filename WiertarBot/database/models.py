from typing import Optional

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column, sessionmaker
from sqlalchemy import String, Text, BigInteger, MetaData

from ..config import database

engine = create_engine(database.url)
Session = sessionmaker(engine)


class Base(DeclarativeBase):
    metadata = MetaData(naming_convention={"ix": "%(column_0_label)s"})


class Permission(Base):
    __tablename__ = "permission"

    id: Mapped[int] = mapped_column(primary_key=True)
    command: Mapped[str] = mapped_column(String(255), index=True, unique=True)
    whitelist: Mapped[str] = mapped_column(Text)
    blacklist: Mapped[str] = mapped_column(Text)


class FBMessage(Base):
    __tablename__ = "fbmessage"

    id: Mapped[int] = mapped_column(primary_key=True)
    message_id: Mapped[str] = mapped_column(String(255), index=True, unique=True)
    thread_id: Mapped[str] = mapped_column(String(255))
    author_id: Mapped[str] = mapped_column(String(255))
    time: Mapped[int] = mapped_column(BigInteger)
    message: Mapped[str] = mapped_column(Text)
    deleted_at: Mapped[Optional[int]] = mapped_column(BigInteger)


class MessageCountMilestone(Base):
    __tablename__ = "messagecountmilestone"

    id: Mapped[int] = mapped_column(primary_key=True)
    thread_id: Mapped[str] = mapped_column(String(255), index=True, unique=True)
    count: Mapped[int]
