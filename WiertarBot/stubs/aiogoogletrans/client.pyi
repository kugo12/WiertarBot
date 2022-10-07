from _typeshed import Incomplete
from aiogoogletrans import urls as urls, utils as utils
from aiogoogletrans.constants import DEFAULT_USER_AGENT as DEFAULT_USER_AGENT, LANGCODES as LANGCODES, LANGUAGES as LANGUAGES, SPECIAL_CASES as SPECIAL_CASES
from aiogoogletrans.gtoken import TokenAcquirer as TokenAcquirer
from aiogoogletrans.models import Detected as Detected, Translated as Translated

EXCLUDES: Incomplete

class Translator:
    headers: Incomplete
    service_urls: Incomplete
    token_acquirer: Incomplete
    proxy: Incomplete
    timeout: Incomplete
    def __init__(self, service_urls: Incomplete | None = ..., user_agent=..., proxy: Incomplete | None = ..., timeout: int = ...) -> None: ...
    async def translate(self, text, dest: str = ..., src: str = ...): ...
    async def detect(self, text): ...
