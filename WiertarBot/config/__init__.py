from .config import *

try:
    from .local_config import *
except ModuleNotFoundError:
    pass
