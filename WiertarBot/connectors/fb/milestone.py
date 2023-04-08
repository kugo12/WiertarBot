import fbchat
import asyncio

from typing import Optional
from collections import defaultdict

from .dispatch import FBEventDispatcher
from .FBContext import FBContext
from ...database import MessageCountMilestoneRepository, MessageCountMilestone

_counts: dict[str, MessageCountMilestone] = {}
_locks: defaultdict[str, asyncio.Lock] = defaultdict(asyncio.Lock)
_default_total_delta = 250_000


def _check_threshold(previous: int, current: int, total_delta: int = _default_total_delta) -> Optional[int]:
    if previous >= current:
        return None

    c = current // total_delta
    if previous // total_delta != c:
        return c * total_delta
    return None


async def _update(thread: fbchat.ThreadABC, context: FBContext) -> Optional[int]:
    async with _locks[thread.id]:
        milestone = _counts.get(thread.id)

        if milestone:
            reached = _check_threshold(milestone.getCount(), milestone.getCount()+1)

            milestone.setCount(milestone.getCount() + 1)
        else:
            count = (await context.fetch_thread(thread.id)).getMessageCount() or 1

            milestone = MessageCountMilestoneRepository.findFirstByThreadId(thread.id) \
                        or MessageCountMilestone.new(thread_id=thread.id, count=count)

            reached = _check_threshold(milestone.getCount(), count)

            milestone.setCount(count)
            _counts[thread.id] = milestone

        MessageCountMilestoneRepository.saveAndFlush(milestone)
    return reached


@FBEventDispatcher.on(fbchat.MessageEvent)
async def milestone_listener(event: fbchat.MessageEvent, *, context: FBContext, **_) -> None:
    if event.thread.id == event.author.id:
        return

    reached = await _update(event.thread, context)
    if reached:
        await event.thread.send_text(f"Gratulacje, osiągnięto ~{reached} wiadomości")
