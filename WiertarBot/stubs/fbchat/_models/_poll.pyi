from .. import _session
from .._common import attrs_default as attrs_default
from typing import Iterable, Sequence

class PollOption:
    id: str
    text: str
    vote: bool
    voters: Sequence[str]
    votes_count: int

class Poll:
    session: _session.Session
    id: str
    question: str
    options: Sequence[PollOption]
    options_count: int
    async def fetch_options(self) -> Sequence[PollOption]: ...
    async def set_votes(self, option_ids: Iterable[str], new_options: Iterable[str] = ...): ...
