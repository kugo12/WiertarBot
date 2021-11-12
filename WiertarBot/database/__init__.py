from .models import *


def init_db():
    db.connect(reuse_if_open=True)
    db.create_tables([Permission, FBMessage])
