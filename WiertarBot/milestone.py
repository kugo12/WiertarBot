import fbchat
import asyncio

from typing import Optional
from collections import defaultdict

from .dispatch import EventDispatcher
from .typing import QueriedMessageCountMilestone
from .database import MilestoneMessageCountRepository
from .abc import Context

_counts: dict[str, QueriedMessageCountMilestone] = {}
_locks: defaultdict[str, asyncio.Lock] = defaultdict(asyncio.Lock)
_default_total_delta = 250_000


def _check_threshold(previous: int, current: int, total_delta: int = _default_total_delta) -> Optional[int]:
    if previous >= current:
        return None

    c = current // total_delta
    if previous // total_delta != c:
        return c * total_delta
    return None


async def _update(thread: fbchat.ThreadABC, context: Context) -> Optional[int]:
    async with _locks[thread.id]:
        milestone = _counts.get(thread.id)

        if milestone:
            reached = _check_threshold(milestone.count, milestone.count+1)

            milestone.count += 1
        else:
            count = (await context.fetch_thread(thread.id)).message_count or 1

            milestone = MilestoneMessageCountRepository.find_by_thread_id(thread.id) \
                        or QueriedMessageCountMilestone.new(thread.id, count)

            reached = _check_threshold(milestone.count, count)

            milestone.count = count
            _counts[thread.id] = milestone

    MilestoneMessageCountRepository.save(milestone)
    return reached


@EventDispatcher.on(fbchat.MessageEvent)
async def milestone_listener(event: fbchat.MessageEvent, *, context: Context, **_) -> None:
    if event.thread.id == event.author.id:
        return

    reached = await _update(event.thread, context)
    if reached:
        await event.thread.send_text(f"Gratulacje, osiągnięto ~{reached} wiadomości")
