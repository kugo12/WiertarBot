from .models import *
from .repositories import *


def init_db() -> None:
    Base.metadata.create_all(engine)


init_db()
