from _typeshed import Incomplete
from aiogoogletrans.constants import DEFAULT_USER_AGENT as DEFAULT_USER_AGENT
from aiogoogletrans.utils import rshift as rshift

class TokenAcquirer:
    RE_TKK: Incomplete
    RE_RAWTKK: Incomplete
    headers: Incomplete
    tkk: Incomplete
    host: Incomplete
    def __init__(self, tkk: str = ..., host: str = ..., user_agent=...) -> None: ...
    def acquire(self, text): ...
    async def do(self, text): ...
