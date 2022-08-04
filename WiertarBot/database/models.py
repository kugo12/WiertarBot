from peewee import PostgresqlDatabase, Model, CharField, TextField, TimestampField

from ..config import database

db = PostgresqlDatabase(
    database.name,
    user=database.user,
    password=database.password,
    host=database.host,
    port=database.port
)


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
