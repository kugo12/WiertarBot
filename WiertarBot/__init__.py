def init_dispatcher():
    from . import message_dispatch
    return message_dispatch.MessageEventDispatcher
