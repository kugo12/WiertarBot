def init_dispatcher():
    from . import commands, message_dispatch
    return message_dispatch.MessageEventDispatcher

