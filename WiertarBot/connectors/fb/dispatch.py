import fbchat

from ...dispatch import EventDispatcher, EventConsumer


def _hook(dispatcher: EventDispatcher, t: type[fbchat.Event], func: EventConsumer) -> None:
    if t.__name__ == 'MessageEvent':
        dispatcher.on(fbchat.MessageReplyEvent)(func)


FBEventDispatcher: EventDispatcher[fbchat.Event] = EventDispatcher(hook=_hook)
