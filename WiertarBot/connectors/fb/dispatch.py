import fbchat

from .FBContext import FBContext
from ...dispatch import EventDispatcher, EventConsumer
from ...events import MessageEvent
from ...message_dispatch import MessageEventDispatcher


def _hook(dispatcher: EventDispatcher, t: type[fbchat.Event], func: EventConsumer) -> None:
    if t.__name__ == 'MessageEvent':
        dispatcher.on(fbchat.MessageReplyEvent)(func)


FBEventDispatcher: EventDispatcher[fbchat.Event] = EventDispatcher(hook=_hook)


@FBEventDispatcher.on(fbchat.MessageEvent)
async def _dispatch_message_event(event: fbchat.MessageEvent, *, context: FBContext, **kwargs) -> None:
    if event.author.id != context.bot_id:
        await MessageEventDispatcher.dispatch(
            MessageEvent.from_fb_event(context, event),
            context=context,
            **kwargs
        )
