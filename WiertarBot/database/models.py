from peewee import *

from ..config import db_path

db = SqliteDatabase(str(db_path))


class BaseModel(Model):
    class Meta:
        database = db


class Permission(BaseModel):
    command = CharField(unique=True)
    whitelist = TextField()
    blacklist = TextField()


class FBMessage(BaseModel):
    message_id = CharField(unique=True)
    thread_id = CharField()
    author_id = CharField()
    time = TimestampField()
    message = TextField()
    deleted_at = TimestampField(null=True)
