from .models import *
from .repositories import *

def init_db():
    db.connect(reuse_if_open=True)
    db.create_tables([Permission, FBMessage])


init_db()
