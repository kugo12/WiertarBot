from _typeshed import Incomplete

class Speed:
    SLOW: bool
    NORMAL: Incomplete

class aiogTTS:
    GOOGLE_TTS_MAX_CHARS: int
    GOOGLE_TTS_HEADERS: Incomplete
    GOOGLE_TTS_RPC: str
    session: Incomplete
    pre_processor_funcs: Incomplete
    tokenizer_func: Incomplete
    def __init__(self, pre_processor_funcs=..., tokenizer_func=...) -> None: ...
    def __del__(self) -> None: ...
    async def write_to_fp(self, text, fp, lang: str = ..., tld: str = ..., slow: bool = ..., lang_check: bool = ...) -> None: ...
    async def save(self, text, filename, lang: str = ..., tld: str = ..., slow: bool = ..., lang_check: bool = ...) -> None: ...

class aiogTTSError(Exception):
    tts: Incomplete
    rsp: Incomplete
    msg: Incomplete
    def __init__(self, msg: Incomplete | None = ..., **kwargs) -> None: ...
    def infer_msg(self, tts, rsp: Incomplete | None = ...): ...
