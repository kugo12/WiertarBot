from . import special
from . import utils
from . import random_images
from . import standard


def reload():
    import importlib
    from sys import modules

    keys = list(modules.keys())
    for key in keys:
        if key.startswith('WiertarBot.commands.'):
            importlib.reload(modules[key])
