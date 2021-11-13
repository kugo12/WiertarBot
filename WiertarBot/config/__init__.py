from .config import *

try:
    from .local_config import *
    from .unlock import *
except ModuleNotFoundError:
    pass
