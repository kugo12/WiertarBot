try:
    from .local_config import *
except ModuleNotFoundError:
    from .config import *
